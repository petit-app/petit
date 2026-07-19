mock_provider "google" {
  override_during = plan

  mock_resource "google_project" {
    defaults = {
      number = "987654321098"
    }
  }

  mock_data "google_project" {
    defaults = {
      number = "123456789012"
    }
  }
}

run "creates_only_an_explicitly_placed_project" {
  command = plan

  variables {
    environment     = "test"
    project_id      = "petit-drive-test"
    create_project  = true
    project_name    = "Petit Drive Test"
    organization_id = "123456789012"
  }

  assert {
    condition     = length(google_project.managed) == 1
    error_message = "Approved project-creation mode must create exactly one project."
  }

  assert {
    condition     = google_project.managed[0].deletion_policy == "PREVENT"
    error_message = "Created projects must be protected against Terraform deletion."
  }
}

run "rejects_project_creation_without_one_parent" {
  command = plan

  variables {
    environment    = "test"
    project_id     = "petit-drive-test"
    create_project = true
    project_name   = "Petit Drive Test"
  }

  expect_failures = [google_project.managed[0]]
}

run "adopts_an_existing_test_project" {
  command = plan

  variables {
    environment = "test"
    project_id  = "petit-drive-test"
  }

  assert {
    condition     = length(google_project.managed) == 0
    error_message = "Existing-project mode must not create a Google Cloud project."
  }

  assert {
    condition = toset(keys(google_project_service.apis)) == toset([
      "cloudresourcemanager.googleapis.com",
      "drive.googleapis.com",
      "serviceusage.googleapis.com",
    ])
    error_message = "Existing-project mode must manage only Cloud Resource Manager, Service Usage, and Drive by default."
  }
}

run "rejects_an_unexpected_existing_project_number" {
  command = plan

  variables {
    environment             = "test"
    project_id              = "example-test-project"
    expected_project_number = "999999999999"
  }

  expect_failures = [data.google_project.existing]
}
