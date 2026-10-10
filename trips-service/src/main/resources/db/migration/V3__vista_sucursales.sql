-- =====================================================================
-- V3 · trips-service · Vista de sucursales (copia local de Fleet)
-- La llenan solo los manejadores de SucursalCreada / SucursalActualizada (paso de eventos):
-- upsert con "version <=" para ignorar eventos viejos o repetidos.
-- Sirve para validar origen y destino de las rutas sin llamar a Fleet y para mostrar nombres.
-- =====================================================================

CREATE TABLE vista_sucursales (
  id         UUID          PRIMARY KEY,           -- sucursalId de Fleet
  nombre     VARCHAR(100)  NOT NULL,
  latitud    NUMERIC(9,6)  NOT NULL,
  longitud   NUMERIC(9,6)  NOT NULL,
  activa     BOOLEAN       NOT NULL,
  version    INT           NOT NULL DEFAULT 0     -- aggregateVersion del último evento aplicado
);
