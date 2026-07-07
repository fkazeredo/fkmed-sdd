---
description: >
  Brings up the full development environment (docker compose db+app; frontend ng serve),
  waits for health UP, smoke-tests through the proxy and presents URLs + dev logins. Use when
  asked to bring up the environment, run the system, test manually, or "subir o ambiente".
  Keywords: dev env, ambiente, subir, run, manual testing, docker compose.
argument-hint: "[--obs to include Grafana/Prometheus/Loki]"
allowed-tools: Read, Bash, Glob, Grep
---

# /dev-env — bring up the development environment

All communication is in **pt-BR**. Do not declare "up and running" without the smoke test in
step 4.

## Steps

1. **Backend + database** (announce: the first time builds the image, ~2-3 min):
   ```bash
   docker compose up -d --build db app
   ```
   Run it in the background. The compose file already orders startup: `db` first
   (healthcheck), then `app`.
2. **Wait for real health** (covers the image build + Spring boot — do not trust "container
   Up"): in the background, an `until` loop over
   `curl -s http://localhost:8080/api/system/health` until it answers `"status":"UP"`.
3. **Frontend**: if `http://localhost:4200/` already answers 200, an `ng serve` is already
   running — **reuse it** (tell the user). Otherwise: `cd frontend && npm start` in the
   background and wait for the 200. The dev-server proxy (`frontend/proxy.conf.json`) routes
   `/api` → `:8080`.
4. **Smoke test (mandatory)**:
   - Health direct: `curl http://localhost:8080/api/system/health` → `UP`.
   - Health **through the proxy**: `curl http://localhost:4200/api/system/health` → `UP`
     (proves frontend↔backend talk to each other).
   - Frontend index → HTTP 200.
5. **Dev logins**: the canonical list lives in the seeder — locate `DevUserSeeder.java` via
   Glob (`backend/src/main/java/**/DevUserSeeder.java`) and present the real table of users
   and roles (shared dev password: `dev12345`; the `dev` super-user has all roles). Do not
   invent users.
6. **`--obs`** (optional): a full `docker compose up -d` also brings the observability stack —
   Grafana at `http://localhost:3000` (`admin`/`admin` in dev), Prometheus `:9090`, Loki.
7. **Report**: a table of URLs + logins + how to shut down:
   - `docker compose down` — stops containers, **keeps** data.
   - `docker compose down -v` — stops and **wipes** the database.
   - `ng serve` is the user's process — stopped in its own terminal (or the one you started
     in the background).

## Notes

- Default ports: app `8080`, db `5432`, frontend `4200` (adjustable via `.env` — see
  `.env.example`). Port taken ⇒ say which process holds it before doing anything.
- Never use the E2E stack (`compose.e2e.yaml`, isolated ports) for manual dev testing — it is
  ephemeral and isolated on purpose.
- **Heavy provider-network seed**: the `app` service runs with `SPRING_PROFILES_ACTIVE:
  dev,devdata` by default, which loads `db/dev/R__seed_dev_provider_network.sql` on top of the
  normal migrations — ~10,000 fictitious providers/doctors across the 27 state capitals, real
  neighborhoods, ~40 specialties (SPEC-0008). It also widens MARIA's plan to `coverage=NACIONAL`
  so the whole base is browsable in Rede. This never runs in E2E/tests/prod (see
  `application-devdata.yaml`); regenerate the SQL with
  `node backend/tools/devseed/generate-provider-seed.mjs` if the seed needs to change.
