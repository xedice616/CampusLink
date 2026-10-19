# Deployment configuration

Files are prepared; no cloud service or payment is provisioned automatically. Existing `npm run dev` + local Spring Boot behavior remains unchanged.

## Local Docker: frontend + Java + PostgreSQL

Start Docker Desktop in Linux-container mode. From the repository root:

```powershell
Copy-Item .env.example .env
# Edit .env: set a nonempty DB_PASSWORD. AI_MODEL/AI_API_KEY are optional.
docker compose config --quiet
docker compose up --build -d
docker compose logs -f backend
```

Open http://localhost:8080 and http://localhost:8080/api/v1/health. Nginx serves the Vite build and forwards `/api` to Java. Database/backend ports are private to the Compose network; only the frontend is published, bound to loopback. PostgreSQL data persists in the `campuslink-postgres` named volume. To stop:

```powershell
docker compose down
```

Do not add `-v` unless you intentionally want to erase the database. `.env` is ignored by Git and excluded from both Docker build contexts. Compose reads it automatically; standalone Spring Boot still requires actual environment variables. Original H2 accounts are not migrated into PostgreSQL. The standalone frontend Docker image expects a Docker-network service named `backend` on port 8787, as configured in compose.yaml.

## Render backend

1. Create PostgreSQL using your selected provider/plan, or use an existing instance. Keep database/backend in a compatible region/network. Review provider pricing/limits before creation; this repository does not provision a paid database.
2. Render -> New -> Blueprint -> select `xedice616/CampusLink`, branch `main`, blueprint `render.yaml`.
3. Supply `DB_URL`, `DB_USER`, `DB_PASSWORD`. DB_URL must be JDBC format, e.g. `jdbc:postgresql://HOST:5432/DATABASE` (append `?sslmode=require` if required by the provider). Do not paste a password-bearing `postgresql://user:password@...` URL here.
4. The blueprint uses backend/Dockerfile with backend/ as build context and `/api/v1/health` as health path. Spring already reads Render's `PORT`. No custom build/start command is required.
5. Optional: set `AI_MODEL` and `AI_API_KEY` in Render -> backend -> Environment. Provider defaults to openai; timeout 60s. Never set these in Vercel/frontend. Without model/key the backend starts normally and AI is explicitly unavailable.
6. Verify `https://YOUR-ACTUAL-BACKEND.onrender.com/api/v1/health`.

The blueprint selects the free web-service plan for an initial demo. Availability/resource limits apply; Java document parsing may require a larger plan. No platform is provisioned by merely committing these files. Choose a suitable persistent PostgreSQL plan separately. Production security review is still required.

Manual Render alternative: New -> Web Service -> Docker -> leave Root Directory empty -> Dockerfile `./backend/Dockerfile` -> Docker context `./backend` -> same environment variables and health path. Blueprint paths are explicit and relative to the repository root.

## Vercel frontend

Replace the explicit placeholder before deploying. From frontend/:

```powershell
node scripts/configure-deploy.mjs https://YOUR-ACTUAL-BACKEND.onrender.com
```

This updates frontend/vercel.json. It accepts only an HTTPS origin and never needs a secret. Inspect, commit and push this file. A real backend hostname cannot be known until your backend exists; the placeholder does not represent a working service. Vercel reads routing configuration before the build, so this must be done before deployment, not inside the Vite build command.

Import the repository into Vercel with Root Directory `frontend`, Framework `Vite`, build `npm run build`, output `dist`. Vercel reads vercel.json from that project root. `/api/:path*` is forwarded to the configured backend; the application already uses hash routes, so an additional SPA catch-all rewrite is unnecessary. The Vite development proxy does not provide production routing.

After deploying, open `https://YOUR-FRONTEND.vercel.app/api/v1/health`, register a test account and verify login/persistence. Use a synthetic PDF/DOCX and explicit consent for a live AI acceptance check. Confirm saved-result ownership, deletion and quota on the deployed PostgreSQL database before inviting real users. CV processing might outlast hosting proxy timeouts/cold starts; check your selected platform's limits if calls fail.

## Files and validation

- backend/Dockerfile: Java 21 multi-stage build and non-root runtime.
- frontend/Dockerfile + nginx.conf: local container build and same-origin proxy.
- .dockerignore in each build context: allowlisted source files; no databases, credentials, dependencies or local builds.
- compose.yaml: persistent PostgreSQL + Java + frontend.
- render.yaml: Docker backend with required database secrets supplied by operator.
- frontend/vercel.json + scripts/configure-deploy.mjs: production routing with an explicit origin setup step.

Configuration validation does not prove image/runtime success. See [deployment preparation checks](DEPLOYMENT_CHECKS.md) and [application verification](VERIFICATION.md) for actual outcomes. Docker image/container execution requires a running Docker daemon. Existing application tests and build commands remain in the main README.

References: [Render Docker](https://render.com/docs/docker), [Render Blueprint spec](https://render.com/docs/blueprint-spec), [Vercel rewrites](https://vercel.com/docs/routing/rewrites).
