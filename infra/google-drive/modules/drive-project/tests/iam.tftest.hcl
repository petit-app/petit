mock_provider "google" {
  override_during = plan

  mock_data "google_project" {
    defaults = {
      number = "123456789012"
    }
  }
}

run "expands_explicit_roles_without_owner_or_editor" {
  command = plan

  variables {
    environment = "prod"
    project_id  = "petit-drive-prod"

    terraform_plan_members      = ["group:example-prod-plan@example.com"]
    terraform_apply_members     = ["serviceAccount:example-prod-apply@example.iam.gserviceaccount.com"]
    oauth_config_editor_members = ["group:petit-oauth@example.com"]
    oauth_config_viewer_members = ["group:petit-oauth-review@example.com"]
    monitoring_viewer_members   = ["group:petit-monitoring@example.com"]
  }

  assert {
    condition = !contains(
      toset([for binding in values(google_project_iam_member.bindings) : binding.role]),
      "roles/owner",
    )
    error_message = "The module must never grant Owner."
  }

  assert {
    condition = !contains(
      toset([for binding in values(google_project_iam_member.bindings) : binding.role]),
      "roles/editor",
    )
    error_message = "The module must never grant Editor."
  }

  assert {
    condition = contains(
      toset([for binding in values(google_project_iam_member.bindings) : "${binding.role}|${binding.member}"]),
      "roles/oauthconfig.editor|group:petit-oauth@example.com",
    )
    error_message = "The manual OAuth operator must receive the purpose-built OAuth Config Editor role."
  }
}

run "rejects_production_apply_members_in_test" {
  command = plan

  variables {
    environment = "test"
    project_id  = "petit-drive-test"
    terraform_apply_members = [
      "serviceAccount:example-prod-apply@example.iam.gserviceaccount.com",
    ]
  }

  expect_failures = [var.terraform_apply_members]
}
