resource "google_monitoring_alert_policy" "drive_api_errors" {
  count = var.enable_drive_api_error_alert ? 1 : 0

  project      = local.project_id
  display_name = "Petit ${var.environment}: Google Drive API ${var.drive_api_error_response_code_class} responses"
  combiner     = "OR"
  enabled      = true

  conditions {
    display_name = "Drive API ${var.drive_api_error_response_code_class} request count"

    condition_threshold {
      filter = join(" AND ", [
        "metric.type=\"serviceruntime.googleapis.com/api/request_count\"",
        "resource.type=\"consumed_api\"",
        "resource.label.\"service\"=\"drive.googleapis.com\"",
        "metric.label.\"response_code_class\"=\"${var.drive_api_error_response_code_class}\"",
      ])
      comparison      = "COMPARISON_GT"
      threshold_value = var.drive_api_error_threshold
      duration        = var.drive_api_error_duration

      aggregations {
        alignment_period     = var.drive_api_error_alignment_period
        per_series_aligner   = "ALIGN_SUM"
        cross_series_reducer = "REDUCE_SUM"
      }

      trigger {
        count = 1
      }
    }
  }

  documentation {
    content   = <<-EOT
      Counts completed Google Drive API requests in the ${var.drive_api_error_response_code_class} response class for project ${local.project_id}.
      This policy does not measure successful Petit backups, OAuth health, per-user Drive storage, or appDataFolder capacity.
    EOT
    mime_type = "text/markdown"
  }

  notification_channels = sort(tolist(var.notification_channels))

  user_labels = {
    environment = var.environment
    service     = "google-drive-api"
  }

  depends_on = [google_project_service.apis]
}
