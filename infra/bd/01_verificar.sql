-- =====================================================================
-- 01_verificar.sql · revisa que cada base y TODAS sus tablas sean de su usuario
-- Correr DESPUÉS de arrancar los servicios (Flyway ya creó las tablas):
--   sudo -u postgres psql -f 01_verificar.sql
-- Si alguna tabla sale con dueño "postgres", esa base se creó antes con el superusuario:
-- usa 00_reiniciar_bases.sql + 00_crear_bases.sql.
-- =====================================================================
\pset footer off

\echo '== Bases y dueños'
SELECT d.datname AS base, pg_get_userbyid(d.datdba) AS dueno
FROM pg_database d WHERE d.datname LIKE 'bd\_%' ORDER BY 1;

\echo '== Migraciones aplicadas y dueño de las tablas (debe ser solo el usr_ de cada base)'
\set resumen 'SELECT current_database() AS base, (SELECT count(*) FROM flyway_schema_history) AS migraciones, (SELECT bool_and(success) FROM flyway_schema_history) AS todas_ok, (SELECT string_agg(DISTINCT tableowner, \',\') FROM pg_tables WHERE schemaname = \'public\') AS duenos_tablas;'
\connect bd_identidad
:resumen
\connect bd_flota
:resumen
\connect bd_viajes
:resumen
\connect bd_ventas
:resumen
\connect bd_mantenimiento
:resumen
\connect bd_reportes
:resumen
\connect bd_notificaciones
:resumen

\connect bd_identidad
\echo '== Admin inicial (lo crea Identity al arrancar)'
SELECT u.correo, u.estado, string_agg(r.codigo, ', ' ORDER BY r.codigo) AS roles
FROM usuarios u
JOIN usuario_roles ur ON ur.usuario_id = u.id
JOIN roles r ON r.codigo = ur.rol_codigo
GROUP BY u.correo, u.estado;
