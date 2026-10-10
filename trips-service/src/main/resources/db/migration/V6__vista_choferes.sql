-- =====================================================================
-- V6 · trips-service · Vista de choferes (copia local de Fleet)
-- La llenan solo los manejadores de ChoferCreado / ChoferActualizado / ChoferDesactivado.
-- Sirve para mostrar el nombre del chofer y llenar selectores. La decisión la toma R1b.
-- nombre_completo VARCHAR(201) igual que en Fleet (nombres + espacio + apellidos).
-- =====================================================================

CREATE TABLE vista_choferes (
  id                  UUID          PRIMARY KEY,          -- choferId de Fleet
  sucursal_id         UUID          NOT NULL,
  nombre_completo     VARCHAR(201)  NOT NULL,
  salario_base_viaje  NUMERIC(12,2) NOT NULL,
  estado              VARCHAR(10)   NOT NULL,             -- ACTIVO, INACTIVO
  version             INT           NOT NULL DEFAULT 0
);

CREATE INDEX ix_vista_choferes_sucursal ON vista_choferes (sucursal_id);
