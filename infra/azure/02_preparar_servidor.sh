#!/bin/bash
# =====================================================================
# 02_preparar_servidor.sh · Prepara la VM de Azure (Ubuntu 24.04)
# Se ejecuta UNA vez DENTRO de la VM:
#   sudo bash 02_preparar_servidor.sh
# PostgreSQL NO va en la VM: es Azure Database for PostgreSQL (servidor flexible) con
# acceso privado en la misma red virtual. Aquí solo se instala el cliente psql.
# Instala Docker (si falta), crea el usuario "deploy" para GitHub Actions y /opt/proyecto2.
# =====================================================================
set -euo pipefail
[ "$(id -u)" -eq 0 ] || { echo "Ejecutar con sudo"; exit 1; }

echo "== Paquetes del sistema y cliente de PostgreSQL"
apt-get update -y
apt-get install -y ca-certificates curl gnupg postgresql-client

echo "== Docker + plugin compose"
if ! command -v docker >/dev/null; then
  curl -fsSL https://get.docker.com | sh
fi
systemctl enable --now docker
docker compose version

echo "== Usuario deploy (lo usa GitHub Actions por SSH)"
id deploy >/dev/null 2>&1 || useradd -m -s /bin/bash deploy
usermod -aG docker deploy
install -d -m 700 -o deploy -g deploy /home/deploy/.ssh
touch /home/deploy/.ssh/authorized_keys
chown deploy:deploy /home/deploy/.ssh/authorized_keys
chmod 600 /home/deploy/.ssh/authorized_keys

echo "== Carpeta de despliegue"
install -d -m 750 -o deploy -g deploy /opt/proyecto2

echo
echo "Listo. Pasos siguientes:"
echo "  1. bash 03_crear_bases.sh <servidor>.postgres.database.azure.com <usuario_admin>"
echo "  2. Pegar la llave pública de GitHub Actions en /home/deploy/.ssh/authorized_keys"
echo "  3. Crear /opt/proyecto2/.env.prod"
