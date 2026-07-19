locals {
  required_services = setunion(
    toset([
      "cloudresourcemanager.googleapis.com",
      "drive.googleapis.com",
      "serviceusage.googleapis.com",
    ]),
    var.enable_drive_api_error_alert ? toset(["monitoring.googleapis.com"]) : toset([]),
  )
}

resource "google_project_service" "apis" {
  for_each = local.required_services

  project                    = local.project_id
  service                    = each.value
  disable_on_destroy         = false
  disable_dependent_services = false
  deletion_policy            = "ABANDON"

  depends_on = [google_project.managed]
}
