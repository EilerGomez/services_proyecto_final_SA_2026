-- =====================================================================
-- V10 · trips-service · Llegadas de viaje (T15)
-- Una fila por viaje que llegó; el viaje pasa a COMPLETADO. Inmutable, como la salida.
-- Guarda los costos ya calculados (los reportes no recalculan con tarifas posteriores):
--   km_recorridos      = km_final - km_inicial de la salida
--   costo_depreciacion = km_recorridos × tarifa_depreciacion (vigente el día de llegada), a 2 decimales
--   salario_chofer     = salario base del viaje, × 1.15 si el viaje es PRIVADO
-- Publica ViajeCompletado.
-- =====================================================================

CREATE TABLE llegadas_viaje (
  viaje_id              UUID          PRIMARY KEY REFERENCES salidas_viaje(viaje_id) ON DELETE RESTRICT,
  fecha_hora_llegada    TIMESTAMPTZ   NOT NULL,      -- fecha elegida por el usuario
  km_final              NUMERIC(12,2) NOT NULL,
  km_recorridos         NUMERIC(12,2) NOT NULL CHECK (km_recorridos >= 0),
  costo_combustible     NUMERIC(12,2) NOT NULL CHECK (costo_combustible >= 0),
  tarifa_depreciacion   NUMERIC(12,4) NOT NULL CHECK (tarifa_depreciacion >= 0),
  costo_depreciacion    NUMERIC(12,2) NOT NULL CHECK (costo_depreciacion >= 0),
  salario_chofer        NUMERIC(12,2) NOT NULL CHECK (salario_chofer > 0),
  registrado_por        UUID          NOT NULL,
  fecha_registro        TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TRIGGER tr_llegadas_inmutables
  BEFORE UPDATE OR DELETE ON llegadas_viaje
  FOR EACH ROW EXECUTE FUNCTION fn_registro_inmutable();
