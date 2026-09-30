#!/usr/bin/env bash
# Run in Google Cloud Shell (or any machine with gcloud + mysql client).
# Fill the variables, then: bash infra/gcp-setup.sh
set -euo pipefail

PROJECT_ID="${PROJECT_ID:?set PROJECT_ID}"
REGION="${REGION:-asia-south1}"
GITHUB_ORG="${GITHUB_ORG:?set GITHUB_ORG (user or org that owns the GitHub repos)}"
GITHUB_API_REPO="${GITHUB_API_REPO:-hotel-os-app}"
ARTIFACT_REPO="${ARTIFACT_REPO:-hotel-os}"
SQL_INSTANCE="${SQL_INSTANCE:-hotel-os-mysql}"
DB_NAME="${DB_NAME:-hotel_os}"
DB_USERNAME="${DB_USERNAME:-hotel_os}"
DB_TIER="${DB_TIER:-db-f1-micro}"

PROJECT_NUMBER="$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')"
DEPLOY_SA="github-deploy@${PROJECT_ID}.iam.gserviceaccount.com"
RUNTIME_SA="hotel-os-runtime@${PROJECT_ID}.iam.gserviceaccount.com"
WIF_POOL="github"
WIF_PROVIDER="github"

echo "==> Enable APIs"
gcloud services enable \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  sqladmin.googleapis.com \
  iamcredentials.googleapis.com \
  iam.googleapis.com \
  secretmanager.googleapis.com \
  cloudresourcemanager.googleapis.com \
  --project "$PROJECT_ID"

echo "==> Artifact Registry"
gcloud artifacts repositories describe "$ARTIFACT_REPO" \
  --location "$REGION" --project "$PROJECT_ID" >/dev/null 2>&1 || \
gcloud artifacts repositories create "$ARTIFACT_REPO" \
  --repository-format=docker \
  --location "$REGION" \
  --project "$PROJECT_ID" \
  --description "Hotel OS Cloud Run images"

echo "==> Service accounts"
gcloud iam service-accounts describe "$DEPLOY_SA" --project "$PROJECT_ID" >/dev/null 2>&1 || \
  gcloud iam service-accounts create github-deploy --display-name "GitHub Cloud Run deploy" --project "$PROJECT_ID"
gcloud iam service-accounts describe "$RUNTIME_SA" --project "$PROJECT_ID" >/dev/null 2>&1 || \
  gcloud iam service-accounts create hotel-os-runtime --display-name "Cloud Run runtime" --project "$PROJECT_ID"

echo "==> IAM for deploy SA"
for ROLE in \
  roles/run.admin \
  roles/artifactregistry.writer \
  roles/iam.serviceAccountUser \
  roles/secretmanager.secretAccessor
do
  gcloud projects add-iam-policy-binding "$PROJECT_ID" \
    --member="serviceAccount:${DEPLOY_SA}" \
    --role="$ROLE" \
    --condition=None >/dev/null
done

gcloud iam service-accounts add-iam-policy-binding "$RUNTIME_SA" \
  --member="serviceAccount:${DEPLOY_SA}" \
  --role="roles/iam.serviceAccountUser" \
  --project "$PROJECT_ID"

echo "==> IAM for runtime SA"
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${RUNTIME_SA}" \
  --role="roles/cloudsql.client" \
  --condition=None >/dev/null
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="serviceAccount:${RUNTIME_SA}" \
  --role="roles/secretmanager.secretAccessor" \
  --condition=None >/dev/null

echo "==> Workload Identity Federation (GitHub Actions)"
gcloud iam workload-identity-pools describe "$WIF_POOL" \
  --location=global --project "$PROJECT_ID" >/dev/null 2>&1 || \
gcloud iam workload-identity-pools create "$WIF_POOL" \
  --location=global \
  --project "$PROJECT_ID" \
  --display-name="GitHub Actions"

gcloud iam workload-identity-pools providers describe "$WIF_PROVIDER" \
  --location=global --workload-identity-pool="$WIF_POOL" --project "$PROJECT_ID" >/dev/null 2>&1 || \
gcloud iam workload-identity-pools providers create-oidc "$WIF_PROVIDER" \
  --location=global \
  --workload-identity-pool="$WIF_POOL" \
  --project "$PROJECT_ID" \
  --display-name="GitHub" \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.repository_owner=assertion.repository_owner" \
  --attribute-condition="assertion.repository_owner=='${GITHUB_ORG}'"

WIF_PROVIDER_RESOURCE="projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${WIF_POOL}/providers/${WIF_PROVIDER}"
POOL_RESOURCE="projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/${WIF_POOL}"

gcloud iam service-accounts add-iam-policy-binding "$DEPLOY_SA" \
  --project "$PROJECT_ID" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/${POOL_RESOURCE}/attribute.repository/${GITHUB_ORG}/${GITHUB_API_REPO}"

echo "==> Cloud SQL MySQL"
gcloud sql instances describe "$SQL_INSTANCE" --project "$PROJECT_ID" >/dev/null 2>&1 || \
gcloud sql instances create "$SQL_INSTANCE" \
  --project "$PROJECT_ID" \
  --database-version=MYSQL_8_0 \
  --tier="$DB_TIER" \
  --region="$REGION" \
  --storage-size=10GB \
  --availability-type=ZONAL \
  --backup-start-time=18:00

gcloud sql databases describe "$DB_NAME" --instance="$SQL_INSTANCE" --project "$PROJECT_ID" >/dev/null 2>&1 || \
  gcloud sql databases create "$DB_NAME" --instance="$SQL_INSTANCE" --project "$PROJECT_ID"

if [[ -z "${DB_PASSWORD:-}" ]]; then
  DB_PASSWORD="$(openssl rand -base64 24)"
  echo "Generated DB_PASSWORD (store it; also written to Secret Manager)."
fi

gcloud sql users list --instance="$SQL_INSTANCE" --project "$PROJECT_ID" --format='value(name)' | grep -qx "$DB_USERNAME" || \
  gcloud sql users create "$DB_USERNAME" \
    --instance="$SQL_INSTANCE" \
    --project "$PROJECT_ID" \
    --password="$DB_PASSWORD"

echo "==> Secret Manager"
ensure_secret() {
  local name="$1"
  local value="$2"
  gcloud secrets describe "$name" --project "$PROJECT_ID" >/dev/null 2>&1 || \
    gcloud secrets create "$name" --project "$PROJECT_ID" --replication-policy=automatic
  printf '%s' "$value" | gcloud secrets versions add "$name" --project "$PROJECT_ID" --data-file=-
}

ensure_secret hotel-os-db-password "$DB_PASSWORD"
ensure_secret hotel-os-jwt-secret "${JWT_SECRET:-$(openssl rand -base64 48)}"
ensure_secret hotel-os-google-client-secret "${GOOGLE_CLIENT_SECRET:-placeholder-replace-me}"

for SECRET in hotel-os-db-password hotel-os-jwt-secret hotel-os-google-client-secret; do
  gcloud secrets add-iam-policy-binding "$SECRET" \
    --project "$PROJECT_ID" \
    --member="serviceAccount:${RUNTIME_SA}" \
    --role="roles/secretmanager.secretAccessor" >/dev/null
done

CLOUDSQL_CONNECTION_NAME="${PROJECT_ID}:${REGION}:${SQL_INSTANCE}"

cat <<EOF

GCP setup finished.

WIF provider (GitHub secret WIF_PROVIDER on ${GITHUB_ORG}/${GITHUB_API_REPO}):
  ${WIF_PROVIDER_RESOURCE}

Deploy service account (GitHub secret WIF_SERVICE_ACCOUNT):
  ${DEPLOY_SA}

GitHub variables on ${GITHUB_ORG}/${GITHUB_API_REPO}:

  GCP_PROJECT_ID=${PROJECT_ID}
  GCP_REGION=${REGION}
  ARTIFACT_REPO=${ARTIFACT_REPO}
  CLOUDSQL_CONNECTION_NAME=${CLOUDSQL_CONNECTION_NAME}
  DB_NAME=${DB_NAME}
  DB_USERNAME=${DB_USERNAME}
  CLOUD_RUN_RUNTIME_SA=${RUNTIME_SA}
  FRONTEND_REPO=${GITHUB_ORG}/hotel-os
  GOOGLE_CLIENT_ID=

Also set secret FRONTEND_CHECKOUT_TOKEN on ${GITHUB_API_REPO} (PAT that can clone hotel-os).
On hotel-os, set BACKEND_DISPATCH_TOKEN (PAT that can dispatch to ${GITHUB_API_REPO}).

Load schema (from this repo, after Cloud SQL is RUNNABLE):

  gcloud sql connect ${SQL_INSTANCE} --user=${DB_USERNAME} --database=${DB_NAME} --project=${PROJECT_ID} < db/schema.sql

Google OAuth authorized redirect URI (after Cloud Run has a URL):

  https://<hotel-os-host>/api/auth/custlogin/oauth2/code/google
EOF
