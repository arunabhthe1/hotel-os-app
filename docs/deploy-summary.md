# Hotel OS — deploy summary

Combined **one Docker image / one Cloud Run service**: the Vite app is built into the Spring Boot JAR. The browser talks to `/api` on the same origin.

```
hotel-os (React)
     │  npm run build → dist/
     ▼
hotel-os-app Docker build
     ├── Spring Boot JAR
     └── static UI
              ▼
     One image → Cloud Run service `hotel-os`
                 (or Docker Compose locally)
```

## What changed

| Area | Change |
|------|--------|
| Architecture | Dropped separate nginx / `hotel-os-web` service |
| API | Serves SPA from `classpath:/static`, health at `/actuator/health`, listens on `PORT` |
| Frontend | Production API URL is empty (same origin). Dev still uses `http://localhost:8080` |
| OAuth | Cloud Run uses request origin (`OAUTH2_USE_REQUEST_ORIGIN=true`) and redirects to `/login` |
| `hotel-os-app` | `Dockerfile`, `docker-compose.yml`, `.github/workflows/deploy-cloudrun.yml`, `infra/gcp-setup.sh` |
| `hotel-os` | Workflow only **dispatches** `hotel-os-app` (does not build its own image) |

GitHub Actions still deploy only from **`main`** (plus manual **Run workflow**). Merge `feature/deploy` into `main` on both repos before the first production deploy.

---

## Local: Docker Compose

Needs Docker Desktop running and sibling checkouts: `hotel-os-app` and `../hotel-os`.

```bash
cd hotel-os-app
copy .env.example .env    # Windows; or cp .env.example .env
docker compose up --build
```

Open http://localhost:8080  
Health: http://localhost:8080/actuator/health

First start can take several minutes (npm + Maven inside the image). If 8080 is dead, Docker Desktop is usually not running.

### Compose DB (app container)

| | Value |
|---|---|
| Host (from PC) | `localhost:5432` |
| Host (from app container) | `postgres:5432` |
| Database | `hotel_os` |
| User | `hotel_os` / `hotel_os` |

Override in `.env` if you copied `.env.example`.

### `mvn spring-boot:run` without Compose

`application.yml` defaults: `hotel_os` / `hotel_os` on `localhost:5432/hotel_os`.

### Demo logins (seeded by the app)

| Role | Email | Password |
|------|--------|----------|
| Customer | `customer@hotel.com` | `customer123` |
| Manager | `manager@hotel.com` | `manager123` |
| Admin | `admin@hotel.com` | `admin123` |

```bash
docker compose down       # stop
docker compose down -v    # stop and delete the Postgres volume
```

---

## Production: GCP (once)

1. Create a GCP project and **enable billing**.
2. In [Cloud Shell](https://shell.cloud.google.com/), clone **hotel-os-app** (or upload `infra/gcp-setup.sh`) and run:

```bash
export PROJECT_ID=your-gcp-project
export GITHUB_ORG=your-github-user-or-org
export REGION=asia-south1
bash infra/gcp-setup.sh
```

Creates Artifact Registry, Cloud SQL PostgreSQL (`hotel-os-pg`), Secret Manager (`hotel-os-db-password`, `hotel-os-jwt-secret`, `hotel-os-google-client-secret`), runtime SA `hotel-os-runtime`, deploy SA `github-deploy`, and Workload Identity Federation for GitHub.

3. When the instance is `RUNNABLE`, load schema:

```bash
gcloud sql connect hotel-os-pg --user=hotel_os --database=hotel_os --project="$PROJECT_ID" < db/schema.sql
```

Cloud SQL user `hotel_os` uses the password stored in Secret Manager (not the Compose password). Cloud SQL keeps costing money even when Cloud Run scales to zero.

You do not create the Cloud Run service by hand. GitHub Actions deploys **`hotel-os`**.

---

## Production: GitHub (required)

Workflow files are already in the repos. You must add secrets/variables and merge to **`main`**.

### PAT(s)

Create a GitHub PAT (classic `repo` scope, or fine-grained: read `hotel-os`, dispatch/write on `hotel-os-app`).

Use the same token for both secrets below if it can access both private repos.

### `hotel-os-app` — Secrets

| Secret | Value |
|--------|--------|
| `WIF_PROVIDER` | Printed by `gcp-setup.sh` (`projects/…/workloadIdentityPools/github/providers/github`) |
| `WIF_SERVICE_ACCOUNT` | `github-deploy@PROJECT_ID.iam.gserviceaccount.com` |
| `FRONTEND_CHECKOUT_TOKEN` | PAT that can clone private `hotel-os` |

### `hotel-os-app` — Variables

| Variable | Example |
|----------|---------|
| `GCP_PROJECT_ID` | `your-gcp-project` |
| `GCP_REGION` | `asia-south1` |
| `ARTIFACT_REPO` | `hotel-os` |
| `CLOUDSQL_CONNECTION_NAME` | `PROJECT_ID:asia-south1:hotel-os-pg` |
| `DB_NAME` | `hotel_os` |
| `DB_USERNAME` | `hotel_os` |
| `CLOUD_RUN_RUNTIME_SA` | `hotel-os-runtime@PROJECT_ID.iam.gserviceaccount.com` |
| `FRONTEND_REPO` | `your-user/hotel-os` (optional if that is the default) |
| `GOOGLE_CLIENT_ID` | optional |
| `VITE_RAZORPAY_KEY_ID` | optional; baked into the UI at image build |

Do **not** put DB password or JWT in GitHub. Cloud Run reads them from Secret Manager.

### `hotel-os` — Secrets / variables

| Name | Type | Value |
|------|------|--------|
| `BACKEND_DISPATCH_TOKEN` | Secret | PAT that can send `repository_dispatch` to `hotel-os-app` |
| `BACKEND_REPO` | Variable (optional) | `your-user/hotel-os-app` |

No GCP WIF secrets on the frontend repo.

### Deploy order

1. Merge `feature/deploy` → `main` on **both** repos.
2. Push **hotel-os-app** `main` (or Actions → **Deploy to Cloud Run** → Run workflow).
3. Open the Cloud Run URL (UI + `/api` on one host).
4. Optional: push **hotel-os** `main` later; that only rebuilds via dispatch.

Google OAuth authorized redirect (after you have the Cloud Run URL):

```
https://<hotel-os-host>/api/auth/custlogin/oauth2/code/google
```

Replace Secret Manager `hotel-os-google-client-secret` if you still have the setup-script placeholder.

---

## Checklist

- [ ] Docker Desktop on; `docker compose up --build` in `hotel-os-app`; UI on :8080
- [ ] GCP project + billing; `bash infra/gcp-setup.sh`
- [ ] Schema loaded into Cloud SQL
- [ ] GitHub secrets/variables on **hotel-os-app** and **hotel-os**
- [ ] Both repos merged to `main`
- [ ] First Actions run on **hotel-os-app** succeeded
- [ ] OAuth redirect URI updated (if using Google login)
