variable "project_id" {
  description = "Dedicated test Google Cloud project ID."
  type        = string
}

variable "expected_project_number" {
  description = "Immutable test project number used to verify the selected project ID."
  type        = string
}

variable "terraform_impersonate_service_account" {
  description = "Environment-specific Terraform executor service account email."
  type        = string

  validation {
    condition     = endswith(var.terraform_impersonate_service_account, ".iam.gserviceaccount.com")
    error_message = "terraform_impersonate_service_account must be a service account email."
  }
}

variable "create_project" {
  description = "Whether project creation has been separately approved. Existing-project adoption is the default."
  type        = bool
  default     = false
}

variable "project_name" {
  description = "Display name required only for approved project creation."
  type        = string
  default     = null
  nullable    = true
}

variable "organization_id" {
  description = "Organization ID for approved project creation."
  type        = string
  default     = null
  nullable    = true
}

variable "folder_id" {
  description = "Folder ID for approved project creation."
  type        = string
  default     = null
  nullable    = true
}

variable "billing_account" {
  description = "Explicit billing account for approved project creation when applicable."
  type        = string
  default     = null
  nullable    = true
}

variable "labels" {
  description = "Non-sensitive labels applied only to a project created by this root."
  type        = map(string)
  default     = {}
}

variable "terraform_test_admin_members" {
  description = "Test-only Terraform administrator principals."
  type        = set(string)
  default     = []
}

variable "oauth_config_editor_members" {
  description = "Manual test OAuth configuration principals."
  type        = set(string)
  default     = []
}

variable "oauth_config_viewer_members" {
  description = "Independent test OAuth reviewer principals."
  type        = set(string)
  default     = []
}

variable "monitoring_viewer_members" {
  description = "Test monitoring reader principals."
  type        = set(string)
  default     = []
}

variable "enable_drive_api_error_alert" {
  description = "Whether the evidence-gated Drive API request-error alert is approved."
  type        = bool
  default     = false
}

variable "metric_emission_confirmed" {
  description = "Whether Drive metric emission was confirmed in this exact test project."
  type        = bool
  default     = false
}

variable "drive_api_error_threshold" {
  description = "Completed Drive error requests allowed per alignment window."
  type        = number
  default     = 5
}

variable "drive_api_error_alignment_period" {
  description = "Alignment window for Drive error requests."
  type        = string
  default     = "300s"
}

variable "drive_api_error_duration" {
  description = "Time the threshold must remain violated."
  type        = string
  default     = "300s"
}

variable "drive_api_error_response_code_class" {
  description = "HTTP response-code class counted by the alert."
  type        = string
  default     = "5xx"
}

variable "notification_channels" {
  description = "Existing test notification-channel resource names."
  type        = set(string)
  default     = []
}
