resource "google_project" "managed" {
  count = var.create_project ? 1 : 0

  name            = var.project_name
  project_id      = var.project_id
  org_id          = var.organization_id
  folder_id       = var.folder_id
  billing_account = var.billing_account
  labels          = var.labels
  deletion_policy = "PREVENT"

  lifecycle {
    precondition {
      condition     = var.project_name != null && trimspace(var.project_name) != ""
      error_message = "project_name is required when create_project is true."
    }

    precondition {
      condition     = (var.organization_id != null) != (var.folder_id != null)
      error_message = "Exactly one of organization_id or folder_id is required when create_project is true."
    }
  }
}

data "google_project" "existing" {
  count = var.create_project ? 0 : 1

  project_id = var.project_id

  lifecycle {
    postcondition {
      condition     = var.expected_project_number == null || tostring(self.number) == var.expected_project_number
      error_message = "Project ${var.project_id} resolved to number ${self.number}, but expected ${coalesce(var.expected_project_number, "an explicitly configured project number")}."
    }
  }
}

locals {
  project_id = var.project_id
  project_number = var.create_project ? (
    google_project.managed[0].number
  ) : data.google_project.existing[0].number
}
