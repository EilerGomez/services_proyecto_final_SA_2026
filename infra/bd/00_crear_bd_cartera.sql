-- =====================================================================
-- 00_crear_bd_cartera.sql · Base del wallet-service en su máquina (PostgreSQL local)
--   sudo -u postgres psql -f 00_crear_bd_cartera.sql
-- Crea usr_cartera y bd_cartera solo si no existen (se puede correr varias veces).
-- La contraseña debe ser la misma de DB_CARTERA_PASSWORD en el .env (por defecto cambiar_cartera).
-- Las tablas NO se crean aquí: las crea Flyway al arrancar wallet-service.
-- Para empezar de cero solo esta base:  sudo -u postgres psql -c "DROP DATABASE IF EXISTS bd_cartera"
-- =====================================================================

SELECT 'CREATE ROLE usr_cartera WITH LOGIN PASSWORD ''cambiar_cartera'''
WHERE NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'usr_cartera')\gexec

SELECT 'CREATE DATABASE bd_cartera OWNER usr_cartera'
WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'bd_cartera')\gexec

REVOKE CONNECT ON DATABASE bd_cartera FROM PUBLIC;

\connect bd_cartera
ALTER SCHEMA public OWNER TO usr_cartera;
