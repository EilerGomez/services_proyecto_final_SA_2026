-- =====================================================================
-- V5 · trips-service · Vista de buses (copia local de Fleet)
-- La llenan solo los manejadores de BusCreado / BusActualizado / BusDesactivado (paso de eventos),
-- con "version <=" para ignorar eventos viejos o repetidos.
-- Sirve para mostrar la placa y llenar selectores. La decisión al programar un viaje la toma R1a.
-- =====================================================================

CREATE TABLE vista_buses (
  id                   UUID         PRIMARY KEY,          -- busId de Fleet
  sucursal_id          UUID         NOT NULL,
  placa                VARCHAR(10)  NOT NULL,
  capacidad_pasajeros  INT          NOT NULL,
  estado               VARCHAR(15)  NOT NULL,             -- ACTIVO, EN_RUTA, EN_TALLER, INACTIVO
  version              INT          NOT NULL DEFAULT 0
);

CREATE INDEX ix_vista_buses_sucursal ON vista_buses (sucursal_id);
