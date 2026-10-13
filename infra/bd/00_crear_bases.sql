-- =====================================================================
-- 00_crear_bases.sql · crea usuarios y bases SOLO si no existen (se puede correr varias veces)
-- Ejecutar como superusuario:
--   sudo -u postgres psql -f 00_crear_bases.sql
-- Las contraseñas deben coincidir con DB_*_PASSWORD del .env.
-- Las tablas NO se crean aquí: cada microservicio las crea con Flyway al arrancar.
-- =====================================================================
\set ON_ERROR_STOP on

-- ---------- Usuarios (crea si falta; si existe, deja la contraseña igual al .env) ----------
DO $$
DECLARE
  u record;
BEGIN
  FOR u IN SELECT * FROM (VALUES
      ('usr_identidad',      'cambiar_identidad'),
      ('usr_flota',          'cambiar_flota'),
      ('usr_viajes',         'cambiar_viajes'),
      ('usr_ventas',         'cambiar_ventas'),
      ('usr_mantenimiento',  'cambiar_mantenimiento'),
      ('usr_reportes',       'cambiar_reportes'),
      ('usr_notificaciones', 'cambiar_notificaciones')) AS t(nombre, clave)
  LOOP
    IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = u.nombre) THEN
      EXECUTE format('CREATE ROLE %I LOGIN PASSWORD %L', u.nombre, u.clave);
      RAISE NOTICE 'Usuario creado: %', u.nombre;
    ELSE
      EXECUTE format('ALTER ROLE %I LOGIN PASSWORD %L', u.nombre, u.clave);
      RAISE NOTICE 'Usuario ya existía (contraseña actualizada): %', u.nombre;
    END IF;
  END LOOP;
END $$;

-- ---------- Bases (CREATE DATABASE no puede ir dentro de DO: se genera y se ejecuta con \gexec) ----------
SELECT format('CREATE DATABASE %I OWNER %I', b.base, b.dueno)
FROM (VALUES
    ('bd_identidad',      'usr_identidad'),
    ('bd_flota',          'usr_flota'),
    ('bd_viajes',         'usr_viajes'),
    ('bd_ventas',         'usr_ventas'),
    ('bd_mantenimiento',  'usr_mantenimiento'),
    ('bd_reportes',       'usr_reportes'),
    ('bd_notificaciones', 'usr_notificaciones')) AS b(base, dueno)
WHERE NOT EXISTS (SELECT 1 FROM pg_database d WHERE d.datname = b.base)
\gexec

-- Si la base ya existía con otro dueño, se le asigna el correcto
ALTER DATABASE bd_identidad      OWNER TO usr_identidad;
ALTER DATABASE bd_flota          OWNER TO usr_flota;
ALTER DATABASE bd_viajes         OWNER TO usr_viajes;
ALTER DATABASE bd_ventas         OWNER TO usr_ventas;
ALTER DATABASE bd_mantenimiento  OWNER TO usr_mantenimiento;
ALTER DATABASE bd_reportes       OWNER TO usr_reportes;
ALTER DATABASE bd_notificaciones OWNER TO usr_notificaciones;

-- Cada servicio solo puede conectarse a su propia base (el dueño sí puede)
REVOKE CONNECT ON DATABASE bd_identidad, bd_flota, bd_viajes, bd_ventas, bd_mantenimiento,
                           bd_reportes, bd_notificaciones FROM PUBLIC;

-- ---------- Resumen ----------
SELECT d.datname AS base, pg_get_userbyid(d.datdba) AS dueno
FROM pg_database d
WHERE d.datname LIKE 'bd\_%'
ORDER BY d.datname;
