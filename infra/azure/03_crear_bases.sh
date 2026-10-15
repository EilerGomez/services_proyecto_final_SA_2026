#!/bin/bash
# =====================================================================
# 03_crear_bases.sh · Crea las 7 bases de producción en Azure Database for PostgreSQL
# Se ejecuta DENTRO de la VM (es la única que llega al servidor: acceso privado):
#   bash 03_crear_bases.sh psql-proyecto2.postgres.database.azure.com p2admin
# Pide la contraseña del administrador del servidor (no se guarda en ningún lado).
# - Un usuario por servicio (usr_identidad, usr_flota, ...) con contraseña aleatoria.
# - Una base por servicio: bd_<x>, cuyo dueño es su usuario. Nadie más puede conectarse.
# - Las TABLAS no se crean aquí: las crea Flyway al arrancar cada servicio.
# - Se puede volver a ejecutar: no duplica nada, pero GENERA CONTRASEÑAS NUEVAS
#   (hay que actualizar .env.prod y reiniciar los servicios).
# Imprime las líneas DB_HOST y DB_*_PASSWORD para pegar en /opt/proyecto2/.env.prod.
# =====================================================================
set -euo pipefail

HOST=${1:?"Uso: bash 03_crear_bases.sh <servidor>.postgres.database.azure.com <usuario_admin>"}
ADMIN=${2:?"Falta el usuario administrador del servidor"}
read -rsp "Contraseña de ${ADMIN}: " PGPASSWORD; echo
export PGPASSWORD PGSSLMODE=require

BASES="identidad flota viajes ventas mantenimiento reportes notificaciones"
psql_az() { psql -h "$HOST" -U "$ADMIN" -d postgres -v ON_ERROR_STOP=1 -tAq "$@"; }

psql_az -c "SELECT 1" >/dev/null || { echo "No se pudo conectar a ${HOST}"; exit 1; }

SALIDA="DB_HOST=${HOST}"$'\n'
for nombre in $BASES; do
  usuario="usr_${nombre}"
  bd="bd_${nombre}"
  clave=$(openssl rand -base64 24 | tr -dc 'A-Za-z0-9' | head -c 24)

  if [ "$(psql_az -c "SELECT 1 FROM pg_roles WHERE rolname='${usuario}'")" = "1" ]; then
    psql_az -c "ALTER ROLE ${usuario} WITH LOGIN PASSWORD '${clave}'"
  else
    psql_az -c "CREATE ROLE ${usuario} WITH LOGIN PASSWORD '${clave}'"
  fi
  # PostgreSQL 16: el administrador (no es superusuario en Azure) necesita ser miembro del
  # rol para poder crear la base con ese dueño.
  psql_az -c "GRANT ${usuario} TO \"${ADMIN}\"" 2>/dev/null || true

  if [ "$(psql_az -c "SELECT 1 FROM pg_database WHERE datname='${bd}'")" != "1" ]; then
    psql_az -c "CREATE DATABASE ${bd} OWNER ${usuario}"
  fi
  psql_az -c "REVOKE CONNECT ON DATABASE ${bd} FROM PUBLIC"

  variable="DB_$(echo "$nombre" | tr '[:lower:]' '[:upper:]')_PASSWORD"
  SALIDA+="${variable}=${clave}"$'\n'
done

echo
echo "Bases creadas:"
psql_az -c "SELECT datname || '  (dueño: ' || pg_get_userbyid(datdba) || ')' FROM pg_database WHERE datname LIKE 'bd\_%' ORDER BY 1"
echo
echo "=== Pegue estas líneas en /opt/proyecto2/.env.prod ==="
printf "%s" "$SALIDA"
