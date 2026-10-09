-- =====================================================================
-- V8 · trips-service · Salidas de viaje (T14)
-- Una fila por viaje que salió; el viaje pasa a EN_RUTA. Inmutable: una salida registrada no se
-- corrige ni se borra (lo pide el enunciado). Publica ViajeIniciado.
-- bus_id y chofer_id son los que realmente salieron (copia del viaje en ese momento).
-- =====================================================================

CREATE TABLE salidas_viaje (
  viaje_id           UUID          PRIMARY KEY REFERENCES viajes(id) ON DELETE RESTRICT,
  bus_id             UUID          NOT NULL,
  chofer_id          UUID          NOT NULL,
  fecha_hora_salida  TIMESTAMPTZ   NOT NULL,         -- fecha elegida por el usuario
  km_inicial         NUMERIC(12,2) NOT NULL CHECK (km_inicial >= 0),
  registrado_por     UUID          NOT NULL,         -- usuarioId del chofer o del admin
  fecha_registro     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TRIGGER tr_salidas_inmutables
  BEFORE UPDATE OR DELETE ON salidas_viaje
  FOR EACH ROW EXECUTE FUNCTION fn_registro_inmutable();
