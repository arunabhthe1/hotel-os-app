# Deploy Hotel OS (API + UI) to Google Cloud Run

One Cloud Run service serves the Spring Boot API and the React build.

```
hotel-os                 hotel-os-app
     │                        │
     │ npm run build          │
     ▼                        │
   dist/ ─────────────────────┤ copied into classpath:/static
                              │
                              │ mvn package
                              ▼
                         One JAR / Docker image
                              │
                              ▼
                         Cloud Run  (hotel-os)
```

The GitHub Action in this repo checks out `hotel-os` into `./frontend`, builds it with an empty API base URL (same origin), copies `dist` into the JAR, pushes Artifact Registry, and deploys Cloud Run.

## One-time GCP setup

1. Create a GCP project and enable billing.
2. In [Cloud Shell](https://shell.cloud.google.com/):

```bash
export PROJECT_ID=your-gcp-project
export GITHUB_ORG=your-github-user-or-org
export REGION=asia-south1
bash infra/gcp-setup.sh
```

3. Load schema when Cloud SQL is `RUNNABLE`:

```bash
gcloud sql connect hotel-os-pg --user=hotel_os --database=hotel_os --project="$PROJECT_ID" < db/schema.sql
```

## GitHub (`hotel-os-app`)

**Secrets**

| Secret | Value |
|--------|--------|
| `WIF_PROVIDER` | `projects/PROJECT_NUMBER/locations/global/workloadIdentityPools/github/providers/github` |
| `WIF_SERVICE_ACCOUNT` | `github-deploy@PROJECT_ID.iam.gserviceaccount.com` |
| `FRONTEND_CHECKOUT_TOKEN` | PAT (or fine-grained token) that can read private `hotel-os` |

`FRONTEND_CHECKOUT_TOKEN` is required if `hotel-os` is private. `GITHUB_TOKEN` cannot clone a second private repo.

**Variables**

| Variable | Example |
|----------|---------|
| `GCP_PROJECT_ID` | `your-gcp-project` |
| `GCP_REGION` | `asia-south1` |
| `ARTIFACT_REPO` | `hotel-os` |
| `CLOUDSQL_CONNECTION_NAME` | `PROJECT_ID:asia-south1:hotel-os-pg` |
| `DB_NAME` | `hotel_os` |
| `DB_USERNAME` | `hotel_os` |
| `CLOUD_RUN_RUNTIME_SA` | `hotel-os-runtime@PROJECT_ID.iam.gserviceaccount.com` |
| `FRONTEND_REPO` | `your-user/hotel-os` (optional; default `{owner}/hotel-os`) |
| `FRONTEND_REF` | `main` (used when this repo is pushed; frontend-changed events pass a SHA) |
| `GOOGLE_CLIENT_ID` | optional |
| `VITE_RAZORPAY_KEY_ID` | optional; baked into the UI at image build |

DB password and JWT stay in Secret Manager.

## GitHub (`hotel-os`)

Frontend pushes do not build their own image. They dispatch this repo.

**Secret:** `BACKEND_DISPATCH_TOKEN` — PAT with permission to send `repository_dispatch` to `hotel-os-app`.

**Variable (optional):** `BACKEND_REPO` — `your-user/hotel-os-app`.

## After first deploy

Open the single Cloud Run URL (UI + `/api`).

Google OAuth redirect:

`https://<hotel-os-host>/api/auth/custlogin/oauth2/code/google`

OAuth return path is `/login` on that same host (`OAUTH2_USE_REQUEST_ORIGIN=true`).

## Local Docker Compose

From `hotel-os-app`, with sibling `../hotel-os`:

```bash
cp .env.example .env
docker compose up --build
```

UI and API: `http://localhost:8080`. Health: `GET /actuator/health`.
