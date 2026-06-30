# Deploy — claims backend on the LAN (same strategy as NearMe/BidHound)

The Android app talks to **`https://danovich.ddns.net:28587`**. On this host that name is served
the same way as the sibling apps (NearMe :28585, BidHound :28586):

1. **LAN DNS** — `bidhound-lan-dns.service` (dnsmasq) resolves `danovich.ddns.net` →
   `192.168.0.115` (this host) for LAN clients.
2. **Direct HTTPS with a self-signed cert** — the Spring Boot backend terminates TLS itself on
   28587 from a mounted PKCS12 keystore. No reverse proxy, no public Let's Encrypt, no
   port-forward.
3. **Boot enablement** — `nearme-startup.sh` (run by `nearme-startup.service`) brings every app
   up with `docker compose up -d` on boot.

## TLS keystore (`deploy/tls/`)

- `claims-tls.p12` — PKCS12 keystore (alias `claims-tls`), mounted into the backend container.
- `claims_tls.pem` — the public cert; also bundled in the Android app so it trusts the backend.

SANs: `danovich.ddns.net`, `localhost`, `192.168.0.115`, `127.0.0.1`. Regenerate (10y):

```bash
cd deploy/tls
openssl req -x509 -newkey rsa:2048 -nodes -days 3650 \
  -keyout claims-key.pem -out claims_tls.pem \
  -subj "/CN=danovich.ddns.net/O=Claims POC" \
  -addext "subjectAltName=DNS:danovich.ddns.net,DNS:localhost,IP:192.168.0.115,IP:127.0.0.1"
openssl pkcs12 -export -inkey claims-key.pem -in claims_tls.pem \
  -name claims-tls -out claims-tls.p12 -passout pass:"$SSL_KEYSTORE_PASSWORD"
rm claims-key.pem
cp claims_tls.pem ../../android/app/src/main/res/raw/claims_tls.pem   # keep the app's copy in sync
```

The keystore password lives in `.env` as `SSL_KEYSTORE_PASSWORD` (gitignored).

## Run it

```bash
# 1. secrets (gitignored): CLAIMS_JWT_SECRET (>=32 bytes), SSL_KEYSTORE_PASSWORD (matches the p12)
cp .env.example .env && $EDITOR .env

# 2. build the fat jar (needs JDK 21 + mavenLocal), then the image
JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64 ./mvnw -pl claims-backend -am -DskipTests package
docker compose build backend

# 3. start backend (+ Postgres dependency)
docker compose up -d backend

# 4. verify HTTPS (self-signed → -k)
curl -sk -o /dev/null -w '%{http_code}\n' https://localhost:28587/api/claims   # 401
```

## Boot enablement — add 28587 to nearme-startup

`deploy/nearme-startup.sh` is the updated startup script (the existing NearMe/BidHound one plus a
`[5b] Start Claims` step and 28587 in the verify/test). Install it (needs root):

```bash
sudo cp /home/vdanovich/projects/ai/deploy/nearme-startup.sh /usr/local/bin/nearme-startup.sh
sudo chmod +x /usr/local/bin/nearme-startup.sh
# the unit (nearme-startup.service) already runs this script on boot — no daemon-reload needed
sudo systemctl restart nearme-startup.service     # apply now
```

## Firewall (ufw)

ufw is active on this host. Allow 28587 the same way 28585/28586 are allowed:

```bash
sudo ufw allow 28587/tcp
sudo ufw status verbose      # confirm
```

(If those ports are scoped to the LAN subnet rather than opened globally, match that scoping for
28587 instead of a blanket allow.)

## Android trust

The app bundles `claims_tls.pem` and trusts it via
`android/app/src/main/res/xml/network_security_config.xml`, so it accepts the self-signed backend
at `danovich.ddns.net:28587`. Keep the bundled `res/raw/claims_tls.pem` in sync with the keystore
if you regenerate the cert.
