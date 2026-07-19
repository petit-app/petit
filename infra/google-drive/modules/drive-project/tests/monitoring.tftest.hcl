mock_provider "google" {
  override_during = plan

  mock_data "google_project" {
    defaults = {
      number = "123456789012"
    }
  }
}

run "creates_only_an_explicitly_confirmed_drive_error_alert" {
  command = plan

  variables {
    environment                  = "test"
    project_id                   = "petit-drive-test"
    enable_drive_api_error_alert = true
    metric_emission_confirmed    = true
    drive_api_error_threshold    = 10
    notification_channels        = ["projects/petit-drive-test/notificationChannels/123"]
  }

  assert {
    condition     = length(google_monitoring_alert_policy.drive_api_errors) == 1
    error_message = "A confirmed and enabled Drive request-error alert must create one policy."
  }

  assert {
    condition     = contains(keys(google_project_service.apis), "monitoring.googleapis.com")
    error_message = "Monitoring API must be enabled only when the alert is enabled."
  }

  assert {
    condition     = strcontains(google_monitoring_alert_policy.drive_api_errors[0].conditions[0].condition_threshold[0].filter, "drive.googleapis.com")
    error_message = "The alert must filter consumed API metrics to drive.googleapis.com."
  }
}
