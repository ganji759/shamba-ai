variable "project_id" {
  description = "GCP project ID."
  type        = string
}

variable "region" {
  description = "GCP region for all resources."
  type        = string
  default     = "europe-west1"
}

variable "service_name" {
  description = "Cloud Run service name."
  type        = string
  default     = "shamba-api"
}

variable "image" {
  description = "Container image to deploy. Leave empty to use a placeholder image on first apply."
  type        = string
  default     = ""
}

variable "api_keys" {
  description = "Comma separated API keys for B2B customers. Stored in Secret Manager."
  type        = string
  sensitive   = true
}

variable "cors_origins" {
  description = "Comma separated allowed web origins."
  type        = string
  default     = ""
}

variable "model_manifest_url" {
  description = "URL of the model manifest, for example a Hugging Face resolve URL."
  type        = string
  default     = ""
}

variable "db_tier" {
  description = "Cloud SQL machine tier. db-f1-micro is the smallest."
  type        = string
  default     = "db-f1-micro"
}

variable "db_deletion_protection" {
  description = "Block deletion of the database by Terraform."
  type        = bool
  default     = true
}

variable "max_instances" {
  description = "Maximum Cloud Run instances."
  type        = number
  default     = 3
}
