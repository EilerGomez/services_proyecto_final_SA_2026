-- =====================================================================
-- V2 · fleet-service · Sucursales
-- Publica SucursalCreada / SucursalActualizada.
-- =====================================================================

CREATE TABLE sucursales (
  id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  codigo               VARCHAR(10)  NOT NULL UNIQUE,
  nombre               VARCHAR(100) NOT NULL UNIQUE,
  departamento         VARCHAR(60)  NOT NULL,
  municipio            VARCHAR(60)  NOT NULL,
  direccion            VARCHAR(255) NOT NULL,
  telefono             VARCHAR(20),
  latitud              NUMERIC(9,6) NOT NULL CHECK (latitud  BETWEEN -90  AND 90),
  longitud             NUMERIC(9,6) NOT NULL CHECK (longitud BETWEEN -180 AND 180),
  activa               BOOLEAN      NOT NULL DEFAULT TRUE,
  version              INT          NOT NULL DEFAULT 0,
  fecha_creacion       TIMESTAMPTZ  NOT NULL DEFAULT now(),
  fecha_actualizacion  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TRIGGER tr_sucursales_fecha
  BEFORE UPDATE ON sucursales
  FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();

CREATE TRIGGER tr_sucursales_no_eliminar
  BEFORE DELETE ON sucursales
  FOR EACH ROW EXECUTE FUNCTION fn_bloquear_eliminacion();
