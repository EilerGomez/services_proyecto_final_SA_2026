-- Migración Flyway V2 · maintenance-service
-- Una vez aplicada NO se modifica: los cambios van en V3__..., V4__...

-- ---------- vista_buses ----------
-- M4 lista los buses filtrando por la sucursal del token
CREATE INDEX ix_vista_buses_sucursal ON vista_buses (sucursal_id);

-- ---------- categorias_gasto ----------
-- Nombre único sin distinguir mayúsculas (Motor y MOTOR chocan)
-- Respalda existsByNombreIgnoreCase si dos altas llegan a la vez (SQLSTATE 23505 da 409)
CREATE UNIQUE INDEX ux_categorias_gasto_nombre_lower ON categorias_gasto (lower(nombre));
