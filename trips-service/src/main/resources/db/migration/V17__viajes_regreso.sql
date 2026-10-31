-- =====================================================================
-- V17 · trips-service · Viaje de regreso y venta de boletos por el piloto
--
-- sentido: un viaje regular de la sucursal A a la B puede tener su viaje de vuelta. El de vuelta NO
--   es otra ruta: es la misma (mismo ruta_id, mismo origen y mismo destino) con sentido = REGRESO.
--   Al mostrarlo, el recorrido se lee al revés (destino → origen). Así no hay que crear rutas espejo
--   ni duplicar precios, y el reporte por ruta sigue sumando la ida y la vuelta en el mismo renglón.
--   Un viaje PRIVADO sale de un alquiler y no tiene vuelta: su sentido siempre es IDA.
--
-- venta_por_chofer: deja que el piloto asignado venda boletos presenciales de ese viaje, útil cuando
--   el bus sale de una sucursal y en la otra punta no hay quien atienda. Sales valida además que el
--   chofer esté activo, que sea el del viaje y que el viaje no haya salido todavía. Los cajeros y el
--   administrador de la sucursal del viaje siguen vendiendo igual, con o sin esta marca.
-- =====================================================================

ALTER TABLE viajes ADD COLUMN sentido VARCHAR(8) NOT NULL DEFAULT 'IDA'
  CHECK (sentido IN ('IDA', 'REGRESO'));

ALTER TABLE viajes ADD COLUMN venta_por_chofer BOOLEAN NOT NULL DEFAULT FALSE;

-- Solo un viaje regular puede ser de regreso: el privado nace de un alquiler de ida (y su retorno,
-- cuando lo hay, ya viene en el propio alquiler como fecha_retorno).
ALTER TABLE viajes ADD CONSTRAINT ck_viajes_sentido_regular CHECK (
  tipo = 'REGULAR' OR sentido = 'IDA');

-- Los listados del piloto y de la ventanilla filtran por esta marca
CREATE INDEX ix_viajes_venta_chofer ON viajes (chofer_id, estado) WHERE venta_por_chofer;
