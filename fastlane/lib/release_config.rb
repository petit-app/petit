require "json"

module PetitRelease
  PACKAGE_NAME = "com.woliveiras.petit".freeze
  TRACK = "alpha".freeze
  RELEASE_STATUS = "completed".freeze
  TEXT_LIMITS = {
    "title.txt" => 30,
    "short_description.txt" => 80,
    "full_description.txt" => 4_000
  }.freeze

  class ConfigurationError < StandardError; end

  module_function

  def read_version(build_file)
    contents = File.read(build_file)
    code = contents[/^\s*versionCode\s*=\s*(\d+)\s*$/, 1]
    name = contents[/^\s*versionName\s*=\s*"([^"]+)"\s*$/, 1]

    raise ConfigurationError, "versionCode is missing from #{build_file}" unless code
    raise ConfigurationError, "versionName is missing from #{build_file}" unless name

    {code: Integer(code, 10), name: name}
  end

  def validate_metadata!(metadata_root, version_code:)
    locales = Dir.children(metadata_root)
      .select { |entry| File.directory?(File.join(metadata_root, entry)) }
      .sort
    raise ConfigurationError, "No metadata locales found in #{metadata_root}" if locales.empty?

    locales.each do |locale|
      locale_root = File.join(metadata_root, locale)
      TEXT_LIMITS.each do |filename, limit|
        validate_text!(File.join(locale_root, filename), limit: limit)
      end
      validate_text!(
        File.join(locale_root, "changelogs", "#{version_code}.txt"),
        limit: 500
      )
      validate_image!(File.join(locale_root, "images", "icon.png"))
      validate_image!(File.join(locale_root, "images", "featureGraphic.png"))

      phone_screenshots = image_files(
        File.join(locale_root, "images", "phoneScreenshots")
      )
      if phone_screenshots.length < 2
        raise ConfigurationError,
          "#{locale} requires at least two phone screenshots"
      end
    end

    locales
  end

  def validate_signing!(repository_root)
    properties_path = File.join(repository_root, "keystore.properties")
    unless File.file?(properties_path)
      raise ConfigurationError,
        "Missing keystore.properties. Copy the tracked template and use a private upload key."
    end

    properties = File.readlines(properties_path, chomp: true).each_with_object({}) do |line, result|
      next if line.strip.empty? || line.lstrip.start_with?("#")

      key, value = line.split("=", 2)
      result[key.strip] = value.to_s.strip
    end
    required = %w[storeFile storePassword keyAlias keyPassword]
    missing = required.select { |key| properties.fetch(key, "").empty? }
    unless missing.empty?
      raise ConfigurationError,
        "keystore.properties is missing required keys: #{missing.join(", ")}"
    end

    keystore = File.expand_path(properties.fetch("storeFile"), repository_root)
    unless File.file?(keystore)
      raise ConfigurationError,
        "The upload keystore referenced by keystore.properties does not exist"
    end

    keystore
  end

  def google_credential_path!(environment = ENV, repository_root: nil)
    path = environment.fetch("GOOGLE_PLAY_JSON_KEY", "").strip
    if path.empty?
      raise ConfigurationError,
        "GOOGLE_PLAY_JSON_KEY must point to a private Google credential JSON file"
    end
    unless File.absolute_path(path) == path
      raise ConfigurationError, "GOOGLE_PLAY_JSON_KEY must be an absolute path"
    end
    unless File.file?(path) && File.readable?(path)
      raise ConfigurationError, "GOOGLE_PLAY_JSON_KEY is not a readable file"
    end
    if repository_root && environment.fetch("GITHUB_ACTIONS", "") != "true"
      credential_path = File.realpath(path)
      repository_path = File.realpath(repository_root)
      if credential_path == repository_path ||
          credential_path.start_with?("#{repository_path}#{File::SEPARATOR}")
        raise ConfigurationError,
          "Local GOOGLE_PLAY_JSON_KEY must be stored outside the repository"
      end
    end

    credential = JSON.parse(File.read(path, encoding: "UTF-8"))
    type = credential.fetch("type", "")
    unless %w[service_account external_account].include?(type)
      raise ConfigurationError,
        "GOOGLE_PLAY_JSON_KEY must contain service_account or external_account credentials"
    end

    path
  rescue JSON::ParserError
    raise ConfigurationError, "GOOGLE_PLAY_JSON_KEY is not valid JSON"
  end

  def validate_no_publishable_duplicates!(store_listing_root)
    duplicates = Dir.glob(
      File.join(store_listing_root, "**", "*"),
      File::FNM_DOTMATCH
    ).select do |path|
      next false unless File.file?(path)
      next false if path.split(File::SEPARATOR).include?("source")

      path.match?(/\.(?:png|jpe?g|zip)\z/i)
    end.sort

    return if duplicates.empty?

    relative = duplicates.map do |path|
      path.delete_prefix("#{store_listing_root}#{File::SEPARATOR}")
    end
    raise ConfigurationError,
      "Publishable store duplicates must move to fastlane/metadata/android: #{relative.join(", ")}"
  end

  def validate_text!(path, limit:)
    raise ConfigurationError, "Missing required metadata file: #{path}" unless File.file?(path)

    contents = File.read(path, encoding: "UTF-8").strip
    raise ConfigurationError, "Metadata file is empty: #{path}" if contents.empty?
    unless contents.valid_encoding?
      raise ConfigurationError, "Metadata file is not valid UTF-8: #{path}"
    end
    if contents.length > limit
      raise ConfigurationError,
        "Metadata file exceeds #{limit} characters: #{path}"
    end
  end
  private_class_method :validate_text!

  def validate_image!(path)
    unless File.file?(path) && File.size?(path)
      raise ConfigurationError, "Missing or empty Play image: #{path}"
    end
  end
  private_class_method :validate_image!

  def image_files(directory)
    return [] unless File.directory?(directory)

    files = Dir.children(directory).sort.each_with_object([]) do |filename, result|
      path = File.join(directory, filename)
      next unless File.file?(path)
      next unless filename.match?(/\.(?:png|jpe?g)\z/i)

      validate_image!(path)
      result << path
    end
    files
  end
  private_class_method :image_files
end
