variable "environment" {
  description = "Isolated deployment environment."
  type        = string

  validation {
    condition     = contains(["test", "prod"], var.environment)
    error_message = "environment must be either test or prod."
  }
}

variable "project_id" {
  description = "Existing or explicitly approved Google Cloud project ID."
  type        = string

  validation {
    condition     = can(regex("^[a-z][a-z0-9-]{4,28}[a-z0-9]$", var.project_id))
    error_message = "project_id must be a valid Google Cloud project ID."
  }
}

variable "expected_project_number" {
  description = "Expected immutable project number used to detect an incorrect project ID before changes are planned."
  type        = string
  default     = null
  nullable    = true

  validation {
    condition     = var.expected_project_number == null || can(regex("^[0-9]+$", var.expected_project_number))
    error_message = "expected_project_number must contain only decimal digits when set."
  }
}

variable "create_project" {
  description = "Whether Terraform may create the project instead of adopting an existing project."
  type        = bool
  default     = false
}

variable "project_name" {
  description = "Display name required only when project creation is approved."
  type        = string
  default     = null
  nullable    = true
}

variable "organization_id" {
  description = "Organization ID used for approved project creation. Mutually exclusive with folder_id."
  type        = string
  default     = null
  nullable    = true
}

variable "folder_id" {
  description = "Folder ID used for approved project creation. Mutually exclusive with organization_id."
  type        = string
  default     = null
  nullable    = true
}

variable "billing_account" {
  description = "Explicit billing account attachment when required for approved project creation."
  type        = string
  default     = null
  nullable    = true
}

variable "labels" {
  description = "Non-sensitive labels applied only to a project created by this module."
  type        = map(string)
  default     = {}
}

variable "terraform_test_admin_members" {
  description = "Test-only Terraform administrators."
  type        = set(string)
  default     = []

  validation {
    condition     = var.environment == "test" || length(var.terraform_test_admin_members) == 0
    error_message = "terraform_test_admin_members may be set only in the test environment."
  }
}

variable "terraform_plan_members" {
  description = "Production principals allowed to inspect project, API, IAM, and monitoring configuration."
  type        = set(string)
  default     = []

  validation {
    condition     = var.environment == "prod" || length(var.terraform_plan_members) == 0
    error_message = "terraform_plan_members may be set only in the prod environment."
  }
}

variable "terraform_apply_members" {
  description = "Production apply identities invoked only after an external approval gate."
  type        = set(string)
  default     = []

  validation {
    condition     = var.environment == "prod" || length(var.terraform_apply_members) == 0
    error_message = "terraform_apply_members may be set only in the prod environment."
  }
}

variable "oauth_config_editor_members" {
  description = "Manual Google Auth Platform operators granted the Beta OAuth Config Editor role."
  type        = set(string)
  default     = []
}

variable "oauth_config_viewer_members" {
  description = "Independent Google Auth Platform reviewers."
  type        = set(string)
  default     = []
}

variable "monitoring_viewer_members" {
  description = "Principals allowed to read environment monitoring."
  type        = set(string)
  default     = []
}

variable "enable_drive_api_error_alert" {
  description = "Whether to enable the separately approved Drive API request-error alert."
  type        = bool
  default     = false
}

variable "metric_emission_confirmed" {
  description = "Explicit confirmation that Drive emitted the filtered Service Runtime metric in this project."
  type        = bool
  default     = false

  validation {
    condition     = !var.enable_drive_api_error_alert || var.metric_emission_confirmed
    error_message = "metric_emission_confirmed must be true before enabling a Drive API alert."
  }
}

variable "drive_api_error_threshold" {
  description = "Completed Drive API error requests allowed per alignment window before alerting."
  type        = number
  default     = 5

  validation {
    condition     = var.drive_api_error_threshold > 0
    error_message = "drive_api_error_threshold must be greater than zero."
  }
}

variable "drive_api_error_alignment_period" {
  description = "Cloud Monitoring alignment window for completed Drive API error requests."
  type        = string
  default     = "300s"

  validation {
    condition     = contains(["60s", "120s", "180s", "240s", "300s", "600s", "900s", "1800s", "3600s"], var.drive_api_error_alignment_period)
    error_message = "drive_api_error_alignment_period must be an approved whole-minute window."
  }
}

variable "drive_api_error_duration" {
  description = "Time the threshold must remain violated before an incident opens."
  type        = string
  default     = "300s"

  validation {
    condition     = contains(["60s", "120s", "180s", "240s", "300s", "600s", "900s", "1800s", "3600s"], var.drive_api_error_duration)
    error_message = "drive_api_error_duration must be an approved whole-minute duration."
  }
}

variable "drive_api_error_response_code_class" {
  description = "HTTP response-code class counted by the Drive API alert."
  type        = string
  default     = "5xx"

  validation {
    condition     = contains(["4xx", "5xx"], var.drive_api_error_response_code_class)
    error_message = "drive_api_error_response_code_class must be either 4xx or 5xx."
  }
}

variable "notification_channels" {
  description = "Existing Cloud Monitoring notification-channel resource names."
  type        = set(string)
  default     = []
}
