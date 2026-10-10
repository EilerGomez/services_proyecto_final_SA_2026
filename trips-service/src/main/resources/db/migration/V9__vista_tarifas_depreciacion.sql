-- =====================================================================
-- V9 · trips-service · Vista de tarifas de depreciación (copia local de Identity)
-- La llena solo el manejador de TarifaDepreciacionCambiada (paso de eventos): INSERT ... ON CONFLICT
-- DO NOTHING (las tarifas no se editan, una nueva es otra fila con otra fecha).
-- T15 usa la tarifa con la vigente_desde más reciente que no pase de la fecha de llegada.
-- =====================================================================

CREATE TABLE vista_tarifas_depreciacion (
  id             UUID          PRIMARY KEY,          -- tarifaId de Identity
  monto_por_km   NUMERIC(12,4) NOT NULL,
  vigente_desde  DATE          NOT NULL UNIQUE
);
