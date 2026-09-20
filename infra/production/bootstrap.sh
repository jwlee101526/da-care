#!/usr/bin/env bash
set -Eeuo pipefail

if ! command -v docker >/dev/null || ! docker compose version >/dev/null 2>&1; then
  apt-get update
  apt-get install -y docker.io docker-compose-v2 curl
fi

if ! command -v curl >/dev/null; then
  apt-get update
  apt-get install -y curl
fi

install -m 755 -d /etc/ssh/sshd_config.d
cat > /etc/ssh/sshd_config.d/00-dacare.conf <<'EOF'
PasswordAuthentication no
KbdInteractiveAuthentication no
PermitRootLogin no
EOF
sshd -t
systemctl reload ssh

for port in 80 443; do
  iptables -C INPUT -p tcp --dport "$port" -j ACCEPT 2>/dev/null || \
    iptables -I INPUT 1 -p tcp --dport "$port" -j ACCEPT
done
if command -v netfilter-persistent >/dev/null; then
  netfilter-persistent save
fi

systemctl enable --now docker
mkdir -p /opt/dacare/runtime /opt/dacare/bin
chmod 750 /opt/dacare /opt/dacare/runtime /opt/dacare/bin
