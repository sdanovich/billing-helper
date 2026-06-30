# Deploy — TLS for the claims backend

The Android app talks to **`https://danovich.ddns.net:28587`**. TLS is terminated by a reverse
proxy (Caddy) in front of the Spring Boot backend (`:8090`). This is a deliberate public inbound
endpoint over DDNS — acceptable only because the data is synthetic and every `/api/**` route is
auth-enforced. It is a **separate door** from the agent's MCP tunnel and changes none of the
tunnel rules.

## Caddy (recommended)

`deploy/Caddyfile` holds the config:

```
danovich.ddns.net:28587 {
    encode gzip
    reverse_proxy localhost:8090
}
```

Run it:

```bash
caddy run --config deploy/Caddyfile
# or install as a service: caddy start --config deploy/Caddyfile
```

Caddy obtains and renews a real Let's Encrypt cert automatically.

### Port forwarding — the one gotcha

The site is **served** on `28587`, but the ACME challenge is **not**. The cert is issued over the
standard challenge ports. So on your router, forward to this machine:

- **28587** → app traffic (required), and
- **80** (HTTP-01) **and/or 443** (TLS-ALPN-01) → so Caddy can complete the ACME challenge.

`danovich.ddns.net` already resolves to this host's public IP. If you cannot expose 80/443, use a
**DNS-01** challenge instead (Caddy DNS provider plugin for your registrar) — then only 28587 needs
forwarding.

### nginx alternative

If you prefer nginx, terminate TLS with a Let's Encrypt cert (via certbot) and `proxy_pass` to
`http://localhost:8090`. Issue the cert with `certbot certonly` (HTTP-01 needs port 80; DNS-01
needs none), then point a `server { listen 28587 ssl; ... }` block at the backend.

## Verifying end-to-end over TLS

```bash
curl -s -o /dev/null -w '%{http_code}\n' https://danovich.ddns.net:28587/api/claims   # 401 (no token)
```

A `401` (not a connection/cert error) means TLS + proxy + auth filter are all working.
