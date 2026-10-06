#!/bin/bash
# =====================================================================
# 02_preparar_servidor.sh · Instala y configura la VM (Ubuntu 24.04)
# Se ejecuta UNA vez DENTRO de la VM:
#   sudo bash 02_preparar_servidor.sh
# Instala Docker y PostgreSQL 16, permite que los contenedores lleguen a PostgreSQL,
# crea el usuario "deploy" para GitHub Actions y la carpeta /opt/proyecto2.
# =====================================================================
set -euo pipefail
[ "$(id -u)" -eq 0 ] || { echo "Ejecutar con sudo"; exit 1; }

echo "== Paquetes del sistema"
apt-get update -y
apt-get upgrade -y
apt-get install -y ca-certificates curl gnupg postgresql postgresql-contrib

echo "== Docker (script oficial) + plugin compose"
if ! command -v docker >/dev/null; then
  curl -fsSL https://get.docker.com | sh
fi
systemctl enable --now docker
docker compose version

echo "== PostgreSQL 16: escuchar conexiones de Docker"
PGCONF=$(ls -d /etc/postgresql/*/main | tail -1)
sed -i "s/^#\?listen_addresses\s*=.*/listen_addresses = '*'/" "$PGCONF/postgresql.conf"
# Solo redes internas de Docker (172.16.0.0/12). Internet no llega: el firewall de GCP no abre 5432.
grep -q "172.16.0.0/12" "$PGCONF/pg_hba.conf" || \
  echo "host    all    all    172.16.0.0/12    scram-sha-256" >> "$PGCONF/pg_hba.conf"
systemctl restart postgresql
systemctl enable postgresql

echo "== Usuario deploy (lo usa GitHub Actions por SSH)"
id deploy >/dev/null 2>&1 || useradd -m -s /bin/bash deploy
usermod -aG docker deploy
install -d -m 700 -o deploy -g deploy /home/deploy/.ssh
touch /home/deploy/.ssh/authorized_keys
chown deploy:deploy /home/deploy/.ssh/authorized_keys
chmod 600 /home/deploy/.ssh/authorized_keys

echo "== Carpeta de despliegue"
install -d -m 750 -o deploy -g deploy /opt/proyecto2
install -d -m 750 -o deploy -g deploy /opt/proyecto2/respaldos

echo "== Respaldo diario de las bases (2:00 a. m., se conservan 7 días)"
cat > /etc/cron.d/proyecto2-respaldos <<'CRON'
0 2 * * * postgres for db in $(psql -tAc "SELECT datname FROM pg_database WHERE datname LIKE 'bd\_%'"); do pg_dump -Fc "$db" > /var/lib/postgresql/respaldo_${db}_$(date +\%F).dump; done; find /var/lib/postgresql -name 'respaldo_*.dump' -mtime +7 -delete
CRON

echo
echo "Listo. Pasos siguientes:"
echo "  1. sudo bash 03_crear_bases.sh            (crea bases y usuarios, imprime contraseñas)"
echo "  2. Pegar la llave pública de GitHub Actions en /home/deploy/.ssh/authorized_keys"
echo "  3. Crear /opt/proyecto2/.env.prod a partir de env.prod.plantilla"
