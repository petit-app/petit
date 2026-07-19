output "project_id" {
  description = "Google Cloud project ID used by this environment."
  value       = local.project_id
}

output "project_number" {
  description = "Google Cloud project number used by this environment."
  value       = local.project_number
}

output "enabled_apis" {
  description = "API services managed without disable-on-destroy behavior."
  value       = sort(tolist(local.required_services))
}

output "drive_api_error_alert_name" {
  description = "Cloud Monitoring alert-policy resource name, or null when monitoring is disabled."
  value       = try(google_monitoring_alert_policy.drive_api_errors[0].name, null)
}

output "oauth_handoff" {
  description = "Non-secret expectations for the manual Google Auth Platform handoff."
  value = {
    environment = var.environment
    package_name = var.environment == "test" ? (
      "com.woliveiras.petit.debug"
    ) : "com.woliveiras.petit"
    certificate_sources = var.environment == "test" ? [
      "debug SHA-1",
      ] : [
      "direct-release SHA-1 when applicable",
      "Google Play App Signing SHA-1",
    ]
    drive_scope = "https://www.googleapis.com/auth/drive.appdata"
  }
}
