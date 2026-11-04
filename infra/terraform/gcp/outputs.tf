output "api_url" {
  description = "Public URL of the API."
  value       = google_cloud_run_v2_service.api.uri
}

output "artifact_registry_repo" {
  description = "Image path prefix. Push to <prefix>/shamba-api:<tag>."
  value       = "${var.region}-docker.pkg.dev/${var.project_id}/${google_artifact_registry_repository.api.repository_id}"
}

output "cloud_sql_connection_name" {
  description = "Cloud SQL connection name."
  value       = google_sql_database_instance.pg.connection_name
}

output "service_account_email" {
  description = "Runtime service account."
  value       = google_service_account.api.email
}
