# Shamba API on Google Cloud

This module creates:

- A Cloud Run service for the API (minimum instances 0).
- An Artifact Registry Docker repository.
- A Cloud SQL Postgres 17 instance (`db-f1-micro`, 10 GB HDD).
- Secret Manager secrets for `API_KEYS` and `DATABASE_URL`.
- A runtime service account. It has `cloudsql.client` and read access to the two secrets only.

Nothing here is applied by CI. You run it by hand.

## Steps

1. Install Terraform 1.6 or later. Run `gcloud auth application-default login`.
2. Copy `terraform.tfvars.example` to `terraform.tfvars`. Set `project_id`, `api_keys` and `cors_origins`.
3. Run:

   ```
   terraform init
   terraform plan -out plan.out
   terraform apply plan.out
   ```

   The first apply deploys a placeholder image. The service will not pass the health check path with it, so the revision may show as unhealthy until you push the API image.
4. Build and push the API image. Run this from the repository root:

   ```
   gcloud auth configure-docker REGION-docker.pkg.dev
   docker build -f services/api/Dockerfile -t "$(terraform -chdir=infra/terraform/gcp output -raw artifact_registry_repo)/shamba-api:v1" .
   docker push "$(terraform -chdir=infra/terraform/gcp output -raw artifact_registry_repo)/shamba-api:v1"
   ```
5. Apply again with `-var image=<pushed image>`.
6. Test: `curl "$(terraform output -raw api_url)/health"`.

To check the files without a cloud account: `terraform init -backend=false && terraform validate && terraform fmt -check`.

## Notes

- Terraform state holds the generated database password. Use a remote state bucket with limited access.
- The Cloud Run service is public. Observation upload needs no key by design. Outbreak routes need `X-API-Key`.
- `db_deletion_protection` is true. Set it to false before `terraform destroy`.
- The database has a public IPv4 address with no authorised networks. Only the Cloud SQL connector reaches it.

## Monthly cost notes

These are rough figures for small traffic. Check current Google Cloud prices.

| Item | Estimate |
|---|---|
| Cloud Run | Near 0 USD. It scales to zero and the free tier covers light use. |
| Cloud SQL `db-f1-micro` | About 8 to 10 USD. It runs all the time and does not scale to zero. |
| Cloud SQL storage and backups | About 1 to 2 USD. |
| Secret Manager, Artifact Registry | Under 1 USD. |

The database is the main cost. To stop the charge, stop the instance when you do not need it, or destroy the stack.
