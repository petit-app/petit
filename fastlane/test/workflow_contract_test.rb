require "minitest/autorun"

class WorkflowContractTest < Minitest::Test
  REPOSITORY_ROOT = File.expand_path("../..", __dir__)
  WORKFLOW_PATH = File.join(
    REPOSITORY_ROOT,
    ".github",
    "workflows",
    "release-alpha.yml"
  )

  def setup
    @workflow = File.read(WORKFLOW_PATH)
  end

  def test_is_manual_and_protected
    assert_match(/^on:\n  workflow_dispatch:\s*$/m, @workflow)
    refute_match(/^\s+(?:push|pull_request):/m, @workflow)
    assert_includes @workflow, "environment: alpha"
    assert_includes @workflow, "cancel-in-progress: false"
    assert_includes @workflow, "refs/heads/main"
    assert_includes @workflow, "refs/tags/v"
  end

  def test_uses_minimal_permissions_and_shared_lane
    assert_includes @workflow, "contents: read"
    assert_includes @workflow, "id-token: write"
    assert_includes @workflow, "bundle exec fastlane android deploy_alpha"
    refute_includes @workflow, "upload-artifact"
  end

  def test_pins_release_actions_and_always_cleans_credentials
    action_uses = @workflow.scan(/uses:\s+([^#\s]+)/).flatten

    refute_empty action_uses
    action_uses.each do |reference|
      assert_match(/@[0-9a-f]{40}\z/, reference)
    end
    assert_includes @workflow, "google-github-actions/auth@"
    assert_includes @workflow, "if: always()"
    assert_includes @workflow, "gha-creds-"
  end
end
