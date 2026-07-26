require "minitest/autorun"
require "tmpdir"

require_relative "../lib/release_config"

class ReleaseConfigTest < Minitest::Test
  REPOSITORY_ROOT = File.expand_path("../..", __dir__)

  def test_release_target_cannot_be_overridden
    assert_equal "com.woliveiras.petit", PetitRelease::PACKAGE_NAME
    assert_equal "alpha", PetitRelease::TRACK
    assert_equal "completed", PetitRelease::RELEASE_STATUS
  end

  def test_reads_version_from_android_build_file
    Dir.mktmpdir do |directory|
      build_file = File.join(directory, "build.gradle.kts")
      File.write(
        build_file,
        <<~KOTLIN,
          android {
            defaultConfig {
              versionCode = 42
              versionName = "2.3.4"
            }
          }
        KOTLIN
      )

      version = PetitRelease.read_version(build_file)

      assert_equal 42, version.fetch(:code)
      assert_equal "2.3.4", version.fetch(:name)
    end
  end

  def test_validates_versioned_locale_metadata
    Dir.mktmpdir do |directory|
      locale = File.join(directory, "pt-BR")
      FileUtils.mkdir_p(File.join(locale, "changelogs"))
      FileUtils.mkdir_p(File.join(locale, "images", "phoneScreenshots"))
      File.write(File.join(locale, "title.txt"), "Petit")
      File.write(File.join(locale, "short_description.txt"), "Pet care")
      File.write(File.join(locale, "full_description.txt"), "Track pet care offline.")
      File.write(File.join(locale, "changelogs", "42.txt"), "Maintenance update.")
      File.binwrite(File.join(locale, "images", "icon.png"), "png")
      File.binwrite(File.join(locale, "images", "featureGraphic.png"), "png")
      File.binwrite(File.join(locale, "images", "phoneScreenshots", "01.png"), "png")
      File.binwrite(File.join(locale, "images", "phoneScreenshots", "02.png"), "png")

      locales = PetitRelease.validate_metadata!(directory, version_code: 42)

      assert_equal ["pt-BR"], locales
    end
  end

  def test_rejects_metadata_without_current_changelog
    Dir.mktmpdir do |directory|
      FileUtils.mkdir_p(File.join(directory, "pt-BR"))

      error = assert_raises(PetitRelease::ConfigurationError) do
        PetitRelease.validate_metadata!(directory, version_code: 42)
      end

      assert_match "title.txt", error.message
    end
  end

  def test_validates_existing_local_signing_files
    Dir.mktmpdir do |directory|
      File.binwrite(File.join(directory, "upload.jks"), "private-test-fixture")
      File.write(
        File.join(directory, "keystore.properties"),
        <<~PROPERTIES
          storeFile=upload.jks
          storePassword=test-only
          keyAlias=upload
          keyPassword=test-only
        PROPERTIES
      )

      keystore = PetitRelease.validate_signing!(directory)

      assert_equal File.join(directory, "upload.jks"), keystore
    end
  end

  def test_rejects_missing_local_signing_configuration
    Dir.mktmpdir do |directory|
      error = assert_raises(PetitRelease::ConfigurationError) do
        PetitRelease.validate_signing!(directory)
      end

      assert_match "Missing keystore.properties", error.message
    end
  end

  def test_accepts_service_account_and_external_account_credentials
    %w[service_account external_account].each do |credential_type|
      Dir.mktmpdir do |directory|
        credential_path = File.join(directory, "#{credential_type}.json")
        File.write(credential_path, JSON.generate("type" => credential_type))

        result = PetitRelease.google_credential_path!(
          {"GOOGLE_PLAY_JSON_KEY" => credential_path}
        )

        assert_equal credential_path, result
      end
    end
  end

  def test_rejects_local_google_credential_inside_repository
    Dir.mktmpdir do |repository_root|
      credential_path = File.join(repository_root, "play-publisher.json")
      File.write(credential_path, JSON.generate("type" => "service_account"))

      error = assert_raises(PetitRelease::ConfigurationError) do
        PetitRelease.google_credential_path!(
          {"GOOGLE_PLAY_JSON_KEY" => credential_path},
          repository_root: repository_root
        )
      end

      assert_match "outside the repository", error.message
    end
  end

  def test_allows_generated_google_credential_inside_github_workspace
    Dir.mktmpdir do |repository_root|
      credential_path = File.join(repository_root, "gha-creds-test.json")
      File.write(credential_path, JSON.generate("type" => "external_account"))

      result = PetitRelease.google_credential_path!(
        {
          "GOOGLE_PLAY_JSON_KEY" => credential_path,
          "GITHUB_ACTIONS" => "true"
        },
        repository_root: repository_root
      )

      assert_equal credential_path, result
    end
  end

  def test_rejects_relative_google_credential_path
    error = assert_raises(PetitRelease::ConfigurationError) do
      PetitRelease.google_credential_path!(
        {"GOOGLE_PLAY_JSON_KEY" => "credentials.json"}
      )
    end

    assert_match "absolute path", error.message
  end

  def test_rejects_publishable_store_duplicates_outside_source_directory
    Dir.mktmpdir do |directory|
      FileUtils.mkdir_p(File.join(directory, "pt-BR", "source"))
      File.binwrite(File.join(directory, "pt-BR", "source", "screen.png"), "png")
      File.binwrite(File.join(directory, "pt-BR", "listing.zip"), "zip")

      error = assert_raises(PetitRelease::ConfigurationError) do
        PetitRelease.validate_no_publishable_duplicates!(directory)
      end

      assert_match "listing.zip", error.message
      refute_match "source/screen.png", error.message
    end
  end

  def test_rejects_oversized_metadata
    Dir.mktmpdir do |directory|
      locale = File.join(directory, "pt-BR")
      FileUtils.mkdir_p(locale)
      File.write(File.join(locale, "title.txt"), "x" * 31)

      error = assert_raises(PetitRelease::ConfigurationError) do
        PetitRelease.validate_metadata!(directory, version_code: 42)
      end

      assert_match "exceeds 30 characters", error.message
    end
  end

  def test_repository_metadata_matches_source_version
    version = PetitRelease.read_version(
      File.join(REPOSITORY_ROOT, "app", "build.gradle.kts")
    )

    locales = PetitRelease.validate_metadata!(
      File.join(REPOSITORY_ROOT, "fastlane", "metadata", "android"),
      version_code: version.fetch(:code)
    )
    PetitRelease.validate_no_publishable_duplicates!(
      File.join(REPOSITORY_ROOT, "docs", "store-listing")
    )

    assert_includes locales, "pt-BR"
  end
end
