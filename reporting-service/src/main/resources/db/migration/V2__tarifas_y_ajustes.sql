-- Migración Flyway V2 · reporting-service
-- Completa el modelo de lectura de bd/06_bd_reportes.sql sin tocar la V1:
-- tabla de tarifas (TarifaDepreciacionCambiada), version en viajes,
-- índices faltantes y longitud de nombre de chofer.

CREATE TABLE tarifas_depreciacion (
  id             UUID          PRIMARY KEY,
  monto_por_km   NUMERIC(12,4) NOT NULL,
  vigente_desde  DATE          NOT NULL UNIQUE
);

ALTER TABLE viajes ADD COLUMN IF NOT EXISTS version INT NOT NULL DEFAULT 0;

ALTER TABLE choferes ALTER COLUMN nombre_completo TYPE VARCHAR(201);

CREATE INDEX IF NOT EXISTS ix_viajes_bus_pendientes ON viajes (bus_id, fecha_salida)
  WHERE estado IN ('PROGRAMADO', 'EN_RUTA');
CREATE INDEX IF NOT EXISTS ix_ingresos_referencia ON ingresos (referencia_id);
