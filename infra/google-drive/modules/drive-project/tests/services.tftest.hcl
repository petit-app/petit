mock_provider "google" {
  override_during = plan

  mock_data "google_project" {
    defaults = {
      number = "123456789012"
    }
  }
}

run "keeps_managed_apis_enabled_on_teardown" {
  command = plan

  variables {
    environment = "prod"
    project_id  = "petit-drive-prod"
  }

  assert {
    condition = alltrue([
      for service in values(google_project_service.apis) :
      !service.disable_on_destroy && service.deletion_policy == "ABANDON"
    ])
    error_message = "Managed APIs must remain enabled when Terraform relinquishes management."
  }

  assert {
    condition = toset(keys(google_project_service.apis)) == toset([
      "cloudresourcemanager.googleapis.com",
      "drive.googleapis.com",
      "serviceusage.googleapis.com",
    ])
    error_message = "The base API set must include Cloud Resource Manager, Drive, and Service Usage only."
  }
}

run "rejects_monitoring_without_confirmed_drive_telemetry" {
  command = plan

  variables {
    environment                  = "test"
    project_id                   = "petit-drive-test"
    enable_drive_api_error_alert = true
    metric_emission_confirmed    = false
  }

  expect_failures = [var.metric_emission_confirmed]
}
