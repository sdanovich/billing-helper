# claims-backend (App #2 — data-side backend)

The HTTP API the clerk's Android app talks to. Owns the synthetic **claims** system of
record (Postgres) and the **scanned document images** (also Postgres). This is a *separate
door* from the agent's MCP tunnel — it reads **and writes** claims data; the agent only reads,
remotely, over the tunnel.

## Boundary rules honored here

- **Images never traverse the MCP tunnel.** They live in a separate `claim_images` table and
  are served only by the authenticated `GET /api/claims/{id}/image`. The structured-claims
  read path never selects image bytes.
- **Synthetic data only.** Ingest rejects non-synthetic ids (claim id must start with `TEST-`,
  member id with `MBR-TEST-`).
- **Images in Postgres, not S3.** LocalStack S3 isn't persistent, so scanned images are stored
  as `bytea`. The `claims.image_id` column references the `claim_images` row.

## Stack

Spring Boot (Java), Postgres + Flyway, platform-stack **per-user** auth
(`backend-login` + `backend-auth`), consumed from the `vendor/platform-stack` submodule via
mavenLocal.

## Auth — per-user login (BidHound parity)

Methods: **email/password**, **Google**, **GitHub**, plus refresh-token-backed **PIN
quick-unlock** on the device. Per-clerk identity via `@CurrentUser`.

Public (no token) endpoints — everything else under `/api/**` requires a Bearer JWT:

| Endpoint | Purpose |
| --- | --- |
| `POST /api/auth/register` | email/password signup |
| `POST /api/auth/login` | email/password login |
| `POST /api/auth/refresh` | rotate refresh token → new JWT |
| `POST /api/auth/google` | Google id-token sign-in |
| `POST /api/auth/github` | GitHub code sign-in |

Claims API (all require a token):

| Endpoint | Purpose |
| --- | --- |
| `GET /api/claims` | list + filter (`status`, `payer`, `member_id`, `q`, `page`, `size`, `sort`) |
| `GET /api/claims/{id}` | full detail |
| `POST /api/claims` | ingest a parsed claim (multipart: `claim` JSON + optional `image` file) |
| `GET /api/claims/{id}/image` | stream the scanned image from Postgres |

## Run it locally

1. **Secrets** — copy `.env.example` to `.env` (repo root) and set at least `CLAIMS_JWT_SECRET`
   (≥ 32 bytes; `openssl rand -base64 48`). Social provider ids are optional (blank disables
   that button; email/password still works).

2. **Postgres**

   ```bash
   docker compose up -d postgres
   ```

3. **Build** (the auth modules are already in `~/.m2`; to rebuild them see the root README)

   ```bash
   ./mvnw -pl claims-backend -am -DskipTests package
   ```

4. **Run** (env from your `.env`)

   ```bash
   set -a; . ./.env; set +a
   ./mvnw -pl claims-backend spring-boot:run
   ```

   The backend listens on `:8090` (plain HTTP, behind the TLS proxy).

## Verify auth (the three-curl check)

```bash
# Register a clerk (public) -> 200 + token
TOKEN=$(curl -s -X POST localhost:8090/api/auth/register \
  -H 'content-type: application/json' \
  -d '{"email":"clerk@example.com","password":"hunter2"}' \
  | python3 -c 'import sys,json;print(json.load(sys.stdin)["token"])')

# No token -> 401
curl -s -o /dev/null -w '%{http_code}\n' localhost:8090/api/claims          # 401

# With token -> 200
curl -s -o /dev/null -w '%{http_code}\n' localhost:8090/api/claims \
  -H "Authorization: Bearer $TOKEN"                                          # 200
```

## TLS / public endpoint

Public URL: **`https://danovich.ddns.net:28587`**. TLS is terminated by **Caddy**
(`deploy/Caddyfile`), which proxies `:28587` → `localhost:8090` and auto-manages a Let's
Encrypt cert. The DDNS name already resolves to this host.

**Port forwarding:** the site is served on `28587`, but the ACME challenge is not — forward
**80 and/or 443** (for HTTP-01 / TLS-ALPN-01) **and** `28587` from the router to this machine.
If 80/443 can't be exposed, use a DNS-01 challenge plugin instead. See `deploy/README.md`.

## Notes

- `ddl-auto=none`: Flyway owns the schema (`db/migration/V1..V3`), including the login
  module's `users` / `refresh_tokens` tables.
- Seed data: `V3` inserts 10 synthetic claims so the list/filter views are populated on first
  run.
