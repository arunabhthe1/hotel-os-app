# Hotel OS App

Spring Boot API for Hotel OS. Sibling of the React app in `../hotel-os`. Schema: [`db/schema.sql`](./db/schema.sql) (same model as `hotel-os/db/schema.sql`).

JWT roles: `customer`, `manager` (front desk), `admin`.

Customer login supports email/password JWT and optional Google OAuth2. Manager and admin login is JWT only.

## Run

```bash
psql -U hotel_os -d hotel_os -f db/schema.sql
mvn spring-boot:run
```

API: `http://localhost:8080`

Override with `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET`.

## Docker Compose

Needs the sibling checkout `../hotel-os`. First start can take a few minutes (npm + Maven inside the image).

```bash
cp .env.example .env
docker compose up --build
```

Open `http://localhost:8080`. PostgreSQL is on `localhost:5432`. Schema is applied on the first empty volume; demo users come from the app seeder.

```bash
docker compose down          # stop
docker compose down -v       # stop and wipe the database volume
```

## Cloud Run

See [docs/deploy-summary.md](./docs/deploy-summary.md) for what changed and the local / GCP / GitHub steps. Detail: [docs/gcp-cloud-run.md](./docs/gcp-cloud-run.md).

Optional customer Google login: set `GOOGLE_CLIENT_ID` and `GOOGLE_CLIENT_SECRET`. Redirect URI:

`http://localhost:8080/api/auth/custlogin/oauth2/code/google`

## Demo accounts

| Role | Email | Password |
|------|-------|----------|
| Customer | `customer@hotel.com` | `customer123` |
| Front desk manager | `manager@hotel.com` | `manager123` |
| Admin | `admin@hotel.com` | `admin123` |

`POST /api/auth/custlogin` returns a Bearer token for **customer** accounts. Staff credentials are rejected.

`GET /api/auth/custlogin/oauth2` lists configured OAuth2 providers. Start Google login at `/api/auth/custlogin/oauth2/authorize/google`. After success the API redirects to the frontend with `?token=<jwt>`.

`POST /api/auth/contolledlogin` returns a Bearer token for **manager** and **admin** accounts. Customer credentials are rejected. OAuth2 is not available on this endpoint.

Send `Authorization: Bearer <token>` on later requests.

`POST /api/auth/register` creates a customer account.

## API surface

| Prefix | Role | Features |
|--------|------|----------|
| `/api/auth` | public `custlogin` (JWT + OAuth2), `contolledlogin` (JWT), register; `/me` authenticated | JWT issue and current user |
| `/api/rooms` | any authenticated role | inventory and date availability |
| `/api/customer` | customer | cart, checkout, own bookings |
| `/api/manager` | manager | modify/cancel, walk-in, room blocks |
| `/api/admin` | admin | base/monthly price, monthly open/close |
