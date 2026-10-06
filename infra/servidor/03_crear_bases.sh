#!/bin/bash
# =====================================================================
# 03_crear_bases.sh · Crea en la VM las 7 bases de producción y sus usuarios
#   sudo bash 03_crear_bases.sh
# - Un usuario por servicio (usr_identidad, usr_flota, ...) con contraseña aleatoria.
# - Una base por servicio: bd_<x>, cuyo dueño es su usuario.
# - Las TABLAS no se crean aquí: las crea Flyway al arrancar cada servicio.
# - Se puede volver a ejecutar: no duplica nada, pero GENERA CONTRASEÑAS NUEVAS
#   (hay que actualizar .env.prod y reiniciar los servicios).
# Imprime las líneas DB_*_PASSWORD para pegar en .env.prod.
# =====================================================================
set -euo pipefail
[ "$(id -u)" -eq 0 ] || { echo "Ejecutar con sudo"; exit 1; }

BASES="identidad flota viajes ventas mantenimiento reportes notificaciones"
psql_pg() { sudo -u postgres psql -v ON_ERROR_STOP=1 -tAq "$@"; }

SALIDA=""
for nombre in $BASES; do
  usuario="usr_${nombre}"
  clave=$(openssl rand -base64 24 | tr -dc 'A-Za-z0-9' | head -c 24)

  if [ "$(psql_pg -c "SELECT 1 FROM pg_roles WHERE rolname='${usuario}'")" = "1" ]; then
    psql_pg -c "ALTER USER ${usuario} WITH PASSWORD '${clave}'"
  else
    psql_pg -c "CREATE USER ${usuario} WITH PASSWORD '${clave}'"
  fi

  for bd in "bd_${nombre}"; do
    if [ "$(psql_pg -c "SELECT 1 FROM pg_database WHERE datname='${bd}'")" != "1" ]; then
      psql_pg -c "CREATE DATABASE ${bd} OWNER ${usuario}"
    fi
    psql_pg -c "REVOKE CONNECT ON DATABASE ${bd} FROM PUBLIC"
  done

  variable="DB_$(echo "$nombre" | tr '[:lower:]' '[:upper:]')_PASSWORD"
  SALIDA+="${variable}=${clave}"$'\n'
done

echo
echo "Bases creadas:"
psql_pg -c "SELECT datname || '  (dueño: ' || pg_get_userbyid(datdba) || ')' FROM pg_database WHERE datname LIKE 'bd\_%' ORDER BY 1"
echo
echo "=== Pegue estas líneas en /opt/proyecto2/.env.prod ==="
printf "%s" "$SALIDA"
