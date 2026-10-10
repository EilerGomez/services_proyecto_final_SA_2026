-- =====================================================================
-- V6 · sales-service · Boletos (uno por asiento de una venta CONFIRMADA)
-- Se crean en S4 (mostrador) o al llegar CarteraDebitada (en línea). precio = precio_boleto del viaje.
-- S5 y ViajeEliminado los pasan a CANCELADO.
-- =====================================================================

CREATE TABLE boletos (
  id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  venta_id         UUID          NOT NULL REFERENCES ventas (id),
  viaje_id         UUID          NOT NULL,
  numero_asiento   INT           NOT NULL CHECK (numero_asiento > 0),
  precio           NUMERIC(12,2) NOT NULL CHECK (precio > 0),
  estado           VARCHAR(10)   NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'CANCELADO'))
);

CREATE INDEX ix_boletos_venta ON boletos (venta_id);
CREATE UNIQUE INDEX ux_boleto_asiento_activo ON boletos (viaje_id, numero_asiento) WHERE estado = 'ACTIVO';
