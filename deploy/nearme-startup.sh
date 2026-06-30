#!/bin/bash
set -e

LOG=/var/log/nearme-startup.log
exec >> "$LOG" 2>&1

echo "===== $(date) Apps startup ====="

echo "[1] Ensure LAN is up"
nmcli radio wifi off || true
nmcli connection up netplan-enp1s0 || true

echo "[2] Ensure DNS override service is running"
systemctl restart bidhound-lan-dns.service
dig @192.168.0.115 danovich.ddns.net +short || true

echo "[3] Start Docker"
systemctl start docker

echo "[4] Start NearMe"
cd /home/vdanovich/projects/nearme
docker compose up -d

echo "[5] Start BidHound"
cd /home/vdanovich/projects/bidhound
docker compose up -d

echo "[5b] Start Claims (App #2) — backend serves HTTPS on 28587 (self-signed)"
cd /home/vdanovich/projects/ai
docker compose up -d backend

echo "[6] Verify ports"
ss -tlnp | grep -E '28585|28586|28587' || true

echo "[7] Test local apps"
curl -k -s https://localhost:28585 || true
curl -k -s https://localhost:28586 || true
curl -k -s https://localhost:28587/api/claims || true

echo "===== done ====="
