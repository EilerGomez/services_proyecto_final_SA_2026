-- =====================================================================
-- V1 · fleet-service · Funciones generales
-- Las usan los triggers de cada tabla (V2 en adelante).
-- Una migración ya aplicada NO se modifica: los cambios van en una versión nueva.
-- =====================================================================

-- Pone la fecha y hora actual en fecha_actualizacion en cada UPDATE
CREATE OR REPLACE FUNCTION fn_actualizar_fecha() RETURNS trigger AS $$
BEGIN
  NEW.fecha_actualizacion := now();
  RETURN NEW;
END; $$ LANGUAGE plpgsql;

-- Impide borrar filas: sucursales, personal, buses y choferes solo se desactivan
CREATE OR REPLACE FUNCTION fn_bloquear_eliminacion() RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'Los registros de % no se eliminan, solo se desactivan', TG_TABLE_NAME;
END; $$ LANGUAGE plpgsql;
