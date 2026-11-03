provider "google" {
  project = var.project_id
  region  = var.region
}

locals {
  apis = [
    "run.googleapis.com",
    "sqladmin.googleapis.com",
    "secretmanager.googleapis.com",
    "artifactregistry.googleapis.com",
  ]
  # Public sample image so the first apply works before you push your own.
  image = var.image != "" ? var.image : "us-docker.pkg.dev/cloudrun/container/hello"
}

resource "google_project_service" "apis" {
  for_each           = toset(local.apis)
  service            = each.value
  disable_on_destroy = false
}

resource "google_artifact_registry_repository" "api" {
  location      = var.region
  repository_id = "shamba"
  format        = "DOCKER"
  description   = "Shamba AI container images"

  cleanup_policies {
    id     = "keep-recent"
    action = "KEEP"
    most_recent_versions {
      keep_count = 5
    }
  }
  cleanup_policies {
    id     = "delete-old"
    action = "DELETE"
    condition {
      older_than = "2592000s"
    }
  }

  depends_on = [google_project_service.apis]
}

# Least privilege: this account only reads its two secrets and connects to Cloud SQL.
resource "google_service_account" "api" {
  account_id   = "${var.service_name}-run"
  display_name = "Shamba API runtime"
}

resource "google_project_iam_member" "cloudsql_client" {
  project = var.project_id
  role    = "roles/cloudsql.client"
  member  = "serviceAccount:${google_service_account.api.email}"
}

resource "random_password" "db" {
  length  = 32
  special = false
}

resource "google_sql_database_instance" "pg" {
  name                = "${var.service_name}-pg"
  region              = var.region
  database_version    = "POSTGRES_17"
  deletion_protection = var.db_deletion_protection

  settings {
    tier              = var.db_tier
    edition           = "ENTERPRISE"
    availability_type = "ZONAL"
    disk_size         = 10
    disk_type         = "PD_HDD"
    disk_autoresize   = true

    backup_configuration {
      enabled = true
    }

    ip_configuration {
      ipv4_enabled = true # Cloud Run connects through the Cloud SQL connector, not by IP.
    }
  }

  depends_on = [google_project_service.apis]
}

resource "google_sql_database" "app" {
  name     = "shamba"
  instance = google_sql_database_instance.pg.name
}

resource "google_sql_user" "app" {
  name     = "shamba"
  instance = google_sql_database_instance.pg.name
  password = random_password.db.result
}

resource "google_secret_manager_secret" "api_keys" {
  secret_id = "${var.service_name}-api-keys"
  replication {
    auto {}
  }
  depends_on = [google_project_service.apis]
}

resource "google_secret_manager_secret_version" "api_keys" {
  secret      = google_secret_manager_secret.api_keys.id
  secret_data = var.api_keys
}

resource "google_secret_manager_secret" "database_url" {
  secret_id = "${var.service_name}-database-url"
  replication {
    auto {}
  }
  depends_on = [google_project_service.apis]
}

resource "google_secret_manager_secret_version" "database_url" {
  secret = google_secret_manager_secret.database_url.id
  # SQLAlchemy URL for the Cloud SQL unix socket that Cloud Run mounts.
  secret_data = "postgresql://${google_sql_user.app.name}:${random_password.db.result}@/${google_sql_database.app.name}?host=/cloudsql/${google_sql_database_instance.pg.connection_name}"
}

resource "google_secret_manager_secret_iam_member" "read" {
  for_each = {
    api_keys     = google_secret_manager_secret.api_keys.secret_id
    database_url = google_secret_manager_secret.database_url.secret_id
  }
  secret_id = each.value
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.api.email}"
}

resource "google_cloud_run_v2_service" "api" {
  name                = var.service_name
  location            = var.region
  ingress             = "INGRESS_TRAFFIC_ALL"
  deletion_protection = false

  template {
    service_account = google_service_account.api.email

    scaling {
      min_instance_count = 0
      max_instance_count = var.max_instances
    }

    volumes {
      name = "cloudsql"
      cloud_sql_instance {
        instances = [google_sql_database_instance.pg.connection_name]
      }
    }

    containers {
      image = local.image

      ports {
        container_port = 8080
      }

      resources {
        limits = {
          cpu    = "1"
          memory = "512Mi"
        }
        cpu_idle = true
      }

      volume_mounts {
        name       = "cloudsql"
        mount_path = "/cloudsql"
      }

      env {
        name  = "CORS_ORIGINS"
        value = var.cors_origins
      }
      env {
        name  = "MODEL_MANIFEST_URL"
        value = var.model_manifest_url
      }
      env {
        name = "API_KEYS"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.api_keys.secret_id
            version = "latest"
          }
        }
      }
      env {
        name = "DATABASE_URL"
        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.database_url.secret_id
            version = "latest"
          }
        }
      }

      startup_probe {
        http_get {
          path = "/health"
        }
        period_seconds    = 5
        failure_threshold = 6
      }
    }
  }

  depends_on = [
    google_secret_manager_secret_iam_member.read,
    google_project_iam_member.cloudsql_client,
    google_secret_manager_secret_version.api_keys,
    google_secret_manager_secret_version.database_url,
  ]
}

# The API is public. Observations need no key and outbreak routes check X-API-Key.
resource "google_cloud_run_v2_service_iam_member" "public" {
  name     = google_cloud_run_v2_service.api.name
  location = google_cloud_run_v2_service.api.location
  role     = "roles/run.invoker"
  member   = "allUsers"
}
