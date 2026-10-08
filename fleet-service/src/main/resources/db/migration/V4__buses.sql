-- =====================================================================
-- V4 · fleet-service · Buses
-- Publica BusCreado / BusActualizado / BusDesactivado.
-- La foto es obligatoria y se guarda en el bucket de GCS; aquí solo la URL.
-- "Desactivar solo sin viajes programados o en ruta" se valida en la aplicación con R3 (Fleet -> Trips).
-- =====================================================================

CREATE TABLE buses (
  id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  sucursal_id          UUID          NOT NULL REFERENCES sucursales(id),
  placa                VARCHAR(10)   NOT NULL UNIQUE,
  marca                VARCHAR(50)   NOT NULL,
  modelo               VARCHAR(50)   NOT NULL,
  anio_fabricacion     INT           NOT NULL CHECK (anio_fabricacion BETWEEN 1950 AND 2100),
  capacidad_pasajeros  INT           NOT NULL CHECK (capacidad_pasajeros BETWEEN 1 AND 120),
  estado               VARCHAR(15)   NOT NULL DEFAULT 'ACTIVO'
                       CHECK (estado IN ('ACTIVO', 'EN_RUTA', 'EN_TALLER', 'INACTIVO')),
  kilometraje_actual   NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (kilometraje_actual >= 0),
  foto_url             VARCHAR(500)  NOT NULL CHECK (btrim(foto_url) <> ''),
  version              INT           NOT NULL DEFAULT 0,
  fecha_creacion       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  fecha_actualizacion  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE INDEX ix_buses_sucursal ON buses (sucursal_id, estado);

CREATE TRIGGER tr_buses_fecha
  BEFORE UPDATE ON buses
  FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();

CREATE TRIGGER tr_buses_no_eliminar
  BEFORE DELETE ON buses
  FOR EACH ROW EXECUTE FUNCTION fn_bloquear_eliminacion();
