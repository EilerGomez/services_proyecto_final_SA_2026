-- =====================================================================
-- V6 · fleet-service · Bitácora de kilometraje
-- Una fila por viaje completado (evento ViajeCompletado de Trips).
-- viaje_id UNIQUE: el mismo viaje no suma kilometraje dos veces aunque el evento llegue repetido.
-- Es un historial: la aplicación solo inserta (la entidad es @Immutable y no hay endpoint para borrar).
-- =====================================================================

CREATE TABLE bitacora_kilometraje (
  id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  bus_id           UUID          NOT NULL REFERENCES buses(id),
  viaje_id         UUID          NOT NULL UNIQUE,
  km_inicial       NUMERIC(12,2) NOT NULL CHECK (km_inicial >= 0),
  km_final         NUMERIC(12,2) NOT NULL,
  fecha_llegada    TIMESTAMPTZ   NOT NULL,
  fecha_registro   TIMESTAMPTZ   NOT NULL DEFAULT now(),
  CHECK (km_final >= km_inicial)
);

-- Últimos viajes de un bus (F11) e historial por fechas (F23)
CREATE INDEX ix_bitacora_bus_llegada ON bitacora_kilometraje (bus_id, fecha_llegada DESC);
