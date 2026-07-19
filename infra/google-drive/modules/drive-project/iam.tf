locals {
  terraform_write_members = setunion(
    var.terraform_test_admin_members,
    var.terraform_apply_members,
  )

  iam_role_members = {
    "roles/browser"                         = setunion(var.terraform_plan_members, local.terraform_write_members)
    "roles/iam.securityReviewer"            = var.terraform_plan_members
    "roles/monitoring.editor"               = local.terraform_write_members
    "roles/monitoring.viewer"               = setunion(var.terraform_plan_members, var.monitoring_viewer_members)
    "roles/oauthconfig.editor"              = var.oauth_config_editor_members
    "roles/oauthconfig.viewer"              = var.oauth_config_viewer_members
    "roles/resourcemanager.projectIamAdmin" = local.terraform_write_members
    "roles/serviceusage.serviceUsageAdmin"  = local.terraform_write_members
    "roles/serviceusage.serviceUsageViewer" = var.terraform_plan_members
  }

  iam_bindings = merge([
    for role, members in local.iam_role_members : {
      for member in members : "${role}|${member}" => {
        role   = role
        member = member
      }
    }
  ]...)
}

resource "google_project_iam_member" "bindings" {
  for_each = local.iam_bindings

  project = local.project_id
  role    = each.value.role
  member  = each.value.member

  depends_on = [google_project.managed]
}
