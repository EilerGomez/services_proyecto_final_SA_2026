-- =====================================================================
-- 00_crear_bases.sql
-- Crea una base de datos y un usuario por microservicio.
-- Ejecutar como superusuario (postgres) una sola vez:
--   psql -U postgres -f 00_crear_bases.sql
-- Las tablas NO se crean a mano: cada script 01..07 es el V1__esquema_inicial.sql
-- de su servicio y Flyway lo ejecuta al arrancar el servicio.
-- Contraseñas de desarrollo; en producción las bases las crea infra/servidor/03_crear_bases.sh.
-- =====================================================================

CREATE USER usr_identidad       WITH PASSWORD 'cambiar_identidad';
CREATE USER usr_flota          WITH PASSWORD 'cambiar_flota';
CREATE USER usr_viajes         WITH PASSWORD 'cambiar_viajes';
CREATE USER usr_ventas         WITH PASSWORD 'cambiar_ventas';
CREATE USER usr_mantenimiento  WITH PASSWORD 'cambiar_mantenimiento';
CREATE USER usr_reportes       WITH PASSWORD 'cambiar_reportes';
CREATE USER usr_notificaciones WITH PASSWORD 'cambiar_notificaciones';

CREATE DATABASE bd_identidad       OWNER usr_identidad;
CREATE DATABASE bd_flota          OWNER usr_flota;
CREATE DATABASE bd_viajes         OWNER usr_viajes;
CREATE DATABASE bd_ventas         OWNER usr_ventas;
CREATE DATABASE bd_mantenimiento  OWNER usr_mantenimiento;
CREATE DATABASE bd_reportes       OWNER usr_reportes;
CREATE DATABASE bd_notificaciones OWNER usr_notificaciones;

-- Cada servicio solo puede conectarse a su propia base.
REVOKE CONNECT ON DATABASE bd_identidad       FROM PUBLIC;
REVOKE CONNECT ON DATABASE bd_flota          FROM PUBLIC;
REVOKE CONNECT ON DATABASE bd_viajes         FROM PUBLIC;
REVOKE CONNECT ON DATABASE bd_ventas         FROM PUBLIC;
REVOKE CONNECT ON DATABASE bd_mantenimiento  FROM PUBLIC;
REVOKE CONNECT ON DATABASE bd_reportes       FROM PUBLIC;
REVOKE CONNECT ON DATABASE bd_notificaciones FROM PUBLIC;