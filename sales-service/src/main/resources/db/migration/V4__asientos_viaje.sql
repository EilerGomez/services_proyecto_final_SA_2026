-- =====================================================================
-- V4 · sales-service · Asientos de cada viaje (1..capacidad)
-- Los crea el manejador de ViajeProgramado (y los que faltan, ViajeActualizado si sube la capacidad).
-- Ciclo: DISPONIBLE → RESERVADO (S2, 5 min) → VENDIDO (CarteraDebitada) | DISPONIBLE (rechazo o vencida)
--        DISPONIBLE → VENDIDO (S4, mostrador) · VENDIDO → DISPONIBLE (S5 cancelar)
-- El bloqueo es un UPDATE condicional "... AND estado = 'DISPONIBLE'": si actualiza menos filas que
-- asientos pedidos, otro cliente ganó → rollback y 409 ASIENTO_OCUPADO.
-- Se borran en cascada con el viaje (ViajeEliminado); ventas y boletos se conservan.
-- =====================================================================

CREATE TABLE asientos_viaje (
  viaje_id         UUID         NOT NULL REFERENCES vista_viajes (viaje_id) ON DELETE CASCADE,
  numero_asiento   INT          NOT NULL CHECK (numero_asiento > 0),
  estado           VARCHAR(12)  NOT NULL DEFAULT 'DISPONIBLE'
                   CHECK (estado IN ('DISPONIBLE', 'RESERVADO', 'VENDIDO')),
  venta_id         UUID,                                -- sin FK: la venta se crea en la misma transacción
  reservado_hasta  TIMESTAMPTZ,
  version          INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (viaje_id, numero_asiento),
  CHECK (estado = 'DISPONIBLE' OR venta_id IS NOT NULL),
  CHECK (estado <> 'RESERVADO' OR reservado_hasta IS NOT NULL)
);

CREATE INDEX ix_asientos_venta ON asientos_viaje (venta_id) WHERE venta_id IS NOT NULL;
CREATE INDEX ix_asientos_reservados ON asientos_viaje (reservado_hasta) WHERE estado = 'RESERVADO';
