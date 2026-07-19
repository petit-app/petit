output "project_id" {
  description = "Production Google Cloud project ID."
  value       = module.drive_project.project_id
}

output "project_number" {
  description = "Production Google Cloud project number."
  value       = module.drive_project.project_number
}

output "enabled_apis" {
  description = "Production APIs managed without disable-on-destroy behavior."
  value       = module.drive_project.enabled_apis
}

output "drive_api_error_alert_name" {
  description = "Production alert-policy resource name, or null when disabled."
  value       = module.drive_project.drive_api_error_alert_name
}

output "oauth_handoff" {
  description = "Non-secret manual production OAuth handoff."
  value       = module.drive_project.oauth_handoff
}
