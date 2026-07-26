require "minitest/autorun"

class FastfileContractTest < Minitest::Test
  REPOSITORY_ROOT = File.expand_path("../..", __dir__)

  def test_required_android_lanes_are_declared
    fastfile = File.read(File.join(REPOSITORY_ROOT, "fastlane", "Fastfile"))

    %w[
      validate_play_credentials
      build_release
      validate_alpha
      deploy_alpha
    ].each do |lane|
      assert_match(/lane\s+:#{lane}\b/, fastfile)
    end
  end

  def test_upload_uses_shared_alpha_configuration
    fastfile = File.read(File.join(REPOSITORY_ROOT, "fastlane", "Fastfile"))

    assert_includes fastfile, "upload_to_play_store("
    assert_includes fastfile, "track: PetitRelease::TRACK"
    assert_includes fastfile, "release_status: PetitRelease::RELEASE_STATUS"
    assert_includes fastfile, "validate_only: validate_only"
    refute_includes fastfile, "track_promote_to"
  end

  def test_version_code_is_checked_without_automatic_mutation
    fastfile = File.read(File.join(REPOSITORY_ROOT, "fastlane", "Fastfile"))

    assert_includes fastfile, "google_play_track_version_codes("
    assert_includes fastfile, 'versionCode #{version_code} has already been used'
    refute_includes fastfile, "increment_version_code"
    refute_includes fastfile, "increment_version_name"
  end

  def test_aab_signature_check_accepts_a_self_signed_upload_certificate
    fastfile = File.read(File.join(REPOSITORY_ROOT, "fastlane", "Fastfile"))

    assert_includes fastfile, 'sh("jarsigner", "-verify", AAB_PATH)'
    refute_includes fastfile, '"-strict"'
  end
end
