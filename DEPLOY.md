# Free 1-week public deployment

| Part | Where | Cost |
|---|---|---|
| React frontend | GitHub Pages (deployed by `.github/workflows/deploy.yml`) | free |
| Spring Boot backend | Render free web service (Docker, deployed by `deploy.yml` via the Render API) | free |
| Oracle database | Oracle Autonomous Database **Always Free** (loaded by `setup-database.yml`) | free |
| 1-week limit | `DEMO_EXPIRES_AT`: backend answers 410 after it; `demo-expiry.yml` suspends Render and replaces the site | — |

Why not Oracle on Render/Railway: Oracle needs about 2 GB of RAM. Render free gives 512 MB, and Railway's trial gives 1 GB.

## Security in place
* HTTPS everywhere (GitHub Pages + Render). The DB uses mutual-TLS with the Oracle wallet and has no open port of its own.
* Demo passwords `admin123` / `faculty123` / `college123` are replaced by secrets. The backend refuses to start in prod without 12+ character passwords.
* CORS only allows your `https://<user>.github.io` origin. Login is limited to 10 tries per IP per 15 min. Tokens expire after 8 h.
* Security headers (CSP, HSTS, nosniff, frame-deny, no-referrer). No SQL in the logs. Internal error text is hidden.
* Secrets live only in GitHub Secrets and Render env vars, and are never written to the repo or the image. The container runs as a non-root user.

## One-time setup (about 20 minutes)

### 1. Oracle Autonomous Database (Always Free)
1. Sign up at cloud.oracle.com (Free Tier; a card is used for verification only).
2. Create Autonomous Database → **Always Free** on → workload *Transaction Processing* → set an ADMIN password (12–30 chars, upper + lower + digit, no `"`).
3. Database connection → **Download wallet** (Instance wallet) → `Wallet_xxx.zip`. Note the service name `xxx_high`.

### 2. Render backend
1. render.com → New → **Web Service** → connect this repo.
2. Root directory `college/backend`, Runtime **Docker**, Instance **Free**, Auto-Deploy **Off**.
3. Environment → **Secret Files** → add `Wallet.zip` (upload the wallet zip).
4. Copy the service URL (e.g. `https://licet-api.onrender.com`) and the service ID (`srv-...`, in the URL of the dashboard page).
5. Account settings → API Keys → create a key.

### 3. GitHub repo settings
**Settings → Pages → Source: GitHub Actions.**

**Settings → Secrets and variables → Actions → Variables**
| Name | Example |
|---|---|
| `API_URL` | `https://licet-api.onrender.com` |
| `DEMO_EXPIRES_AT` | `2026-11-07T18:30:00Z` (7 days from start, UTC) |

**Secrets**
| Name | Value |
|---|---|
| `ADB_WALLET_B64` | `base64 -w0 Wallet_xxx.zip` (macOS: `base64 -i Wallet_xxx.zip`) |
| `ADB_SERVICE` | `xxx_high` |
| `ADB_ADMIN_PASSWORD` | the ADMIN password from step 1 |
| `COLLEGE_DB_PASSWORD` | new strong password for the COLLEGE schema (12+ chars, upper/lower/digit, no `"`) |
| `ADMIN_PASSWORD` | web login password for `admin` (12+ chars) |
| `FACULTY_PASSWORD` | web login password for `faculty` (12+ chars) |
| `RENDER_API_KEY` | Render API key |
| `RENDER_SERVICE_ID` | `srv-...` |

### 4. Run
1. Actions → **Setup database** → Run workflow → type `RESET` (loads tables, PL/SQL and demo data).
2. Actions → **Deploy** → Run workflow (it also runs on every push to `main`).
3. Open `https://<user>.github.io/<repo>/` and log in as `admin` with your `ADMIN_PASSWORD`.

The demo stops by itself at `DEMO_EXPIRES_AT`. To stop it early: Actions → **Demo expiry** → Run → type `END`.
