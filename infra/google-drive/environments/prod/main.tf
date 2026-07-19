module "drive_project" {
  source = "../../modules/drive-project"

  environment             = "prod"
  project_id              = var.project_id
  expected_project_number = var.expected_project_number
  create_project          = var.create_project
  project_name            = var.project_name
  organization_id         = var.organization_id
  folder_id               = var.folder_id
  billing_account         = var.billing_account
  labels                  = var.labels

  terraform_plan_members      = var.terraform_plan_members
  terraform_apply_members     = var.terraform_apply_members
  oauth_config_editor_members = var.oauth_config_editor_members
  oauth_config_viewer_members = var.oauth_config_viewer_members
  monitoring_viewer_members   = var.monitoring_viewer_members

  enable_drive_api_error_alert        = var.enable_drive_api_error_alert
  metric_emission_confirmed           = var.metric_emission_confirmed
  drive_api_error_threshold           = var.drive_api_error_threshold
  drive_api_error_alignment_period    = var.drive_api_error_alignment_period
  drive_api_error_duration            = var.drive_api_error_duration
  drive_api_error_response_code_class = var.drive_api_error_response_code_class
  notification_channels               = var.notification_channels
}
