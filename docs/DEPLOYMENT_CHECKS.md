# Deployment preparation checks

Actual checks on 19 October 2026:

- `docker compose config --quiet`: passed with a temporary, synthetic DB_PASSWORD; no real credentials written or printed.
- `npm run build`: passed (8 Vite modules; production assets emitted).
- `node --check frontend/scripts/configure-deploy.mjs`: passed.
- Backend-origin configuration: valid HTTPS origin accepted; missing, HTTP, path-bearing, credential-bearing, query-bearing and placeholder origins rejected. Checked on a temporary copy so the project's explicit placeholder remains intact.
- Render YAML parsed successfully; Docker context/file paths exist; health-check route and required database-secret entries checked. This was not an authenticated Render Blueprint API validation.
- Docker image build and Compose startup: **not run**. Docker CLI exists, but Docker Desktop Linux engine is unavailable (`dockerDesktopLinuxEngine` named pipe not found). Start Docker Desktop, then follow DEPLOYMENT.md to build/run the stack.
- No Render/Vercel deployment, database provisioning, credential entry or paid cloud resource creation was performed.

The existing 20 backend tests and 4 browser tests are documented separately in VERIFICATION.md; backend/application behavior was not changed by this configuration-only addition. Containerized PostgreSQL behavior and real hosting still need runtime acceptance checks.
