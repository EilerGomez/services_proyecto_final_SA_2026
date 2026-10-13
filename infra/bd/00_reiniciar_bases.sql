-- =====================================================================
-- 00_reiniciar_bases.sql · BORRA las 7 bases y las crea vacías (prueba desde cero)
-- Úsalo para la prueba completa: cada servicio vuelve a crear sus tablas con Flyway al arrancar
-- y el admin inicial lo crea Identity. Se pierden todos los datos de estas 7 bases.
--   1. Detén los servicios:     docker compose --profile app down
--   2. sudo -u postgres psql -f 00_reiniciar_bases.sql
--   3. sudo -u postgres psql -f 00_crear_bases.sql
-- WITH (FORCE) cierra las conexiones que queden abiertas (PostgreSQL 13 o mayor).
-- =====================================================================
\set ON_ERROR_STOP on

DROP DATABASE IF EXISTS bd_identidad      WITH (FORCE);
DROP DATABASE IF EXISTS bd_flota          WITH (FORCE);
DROP DATABASE IF EXISTS bd_viajes         WITH (FORCE);
DROP DATABASE IF EXISTS bd_ventas         WITH (FORCE);
DROP DATABASE IF EXISTS bd_mantenimiento  WITH (FORCE);
DROP DATABASE IF EXISTS bd_reportes       WITH (FORCE);
DROP DATABASE IF EXISTS bd_notificaciones WITH (FORCE);

\echo 'Bases borradas. Ahora corre 00_crear_bases.sql'
