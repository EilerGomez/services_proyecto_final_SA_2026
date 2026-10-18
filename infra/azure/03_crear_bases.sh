#!/bin/bash
# =====================================================================
# 03_crear_bases.sh · Crea las bases de producción en Azure Database for PostgreSQL
# Se ejecuta DENTRO de la VM (es la única que llega al servidor: acceso privado):
#   bash 03_crear_bases.sh psql-proyecto2.postgres.database.azure.com p2admin            -> las 8 bases
#   bash 03_crear_bases.sh psql-proyecto2.postgres.database.azure.com p2admin cartera    -> solo bd_cartera
# Pide la contraseña del administrador del servidor (no se guarda en ningún lado).
# - Un usuario por servicio (usr_identidad, usr_flota, ...) con contraseña aleatoria.
# - Una base por servicio: bd_<x>, cuyo dueño es su usuario. Nadie más puede conectarse.
# - El esquema public queda a nombre del usuario del servicio (PostgreSQL 15+ ya no deja crear
#   tablas en public a cualquiera, y en Azure ese esquema no es del dueño de la base).
# - Las TABLAS no se crean aquí: las crea Flyway al arrancar cada servicio.
# - Se puede volver a ejecutar: no duplica nada, pero GENERA CONTRASEÑAS NUEVAS para las bases
#   indicadas (hay que actualizarlas en .env.prod y reiniciar esos servicios). Por eso, para agregar
#   una base nueva, pásela como tercer argumento y no se tocan las demás.
# Imprime las líneas DB_HOST y DB_*_PASSWORD para pegar en /opt/proyecto2/.env.prod.
# =====================================================================
set -euo pipefail

HOST=${1:?"Uso: bash 03_crear_bases.sh <servidor>.postgres.database.azure.com <usuario_admin> [bases...]"}
ADMIN=${2:?"Falta el usuario administrador del servidor"}
shift 2
BASES=${*:-"identidad flota viajes ventas mantenimiento reportes notificaciones cartera"}

read -rsp "Contraseña de ${ADMIN}: " PGPASSWORD; echo
export PGPASSWORD PGSSLMODE=require

psql_az() { psql -h "$HOST" -U "$ADMIN" -d postgres -v ON_ERROR_STOP=1 -tAq "$@"; }
psql_bd() { local bd=$1; shift; psql -h "$HOST" -U "$ADMIN" -d "$bd" -v ON_ERROR_STOP=1 -tAq "$@"; }

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
  # Flyway crea las tablas en public: el usuario del servicio debe ser dueño del esquema
  psql_bd "$bd" -c "ALTER SCHEMA public OWNER TO ${usuario}" \
    || psql_bd "$bd" -c "GRANT USAGE, CREATE ON SCHEMA public TO ${usuario}"

  variable="DB_$(echo "$nombre" | tr '[:lower:]' '[:upper:]')_PASSWORD"
  SALIDA+="${variable}=${clave}"$'\n'
done

echo
echo "Bases en el servidor:"
psql_az -c "SELECT datname || '  (dueño: ' || pg_get_userbyid(datdba) || ')' FROM pg_database WHERE datname LIKE 'bd\_%' ORDER BY 1"
unset PGPASSWORD
echo
echo "=== Pegue estas líneas en /opt/proyecto2/.env.prod ==="
printf "%s" "$SALIDA"
