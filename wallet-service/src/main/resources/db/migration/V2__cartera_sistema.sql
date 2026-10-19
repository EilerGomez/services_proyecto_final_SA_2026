-- =====================================================================
-- V2__cartera_sistema.sql  ·  Wallet Service
-- Billetera del sistema: no es de ningún usuario. Recibe el dinero de las compras en línea
-- (boletos y alquileres pagados con cartera) y de ella sale cada reembolso.
-- Solo la consulta el administrador del sistema. Arranca en Q0.
-- =====================================================================

ALTER TABLE carteras
  ADD COLUMN tipo VARCHAR(10) NOT NULL DEFAULT 'USUARIO' CHECK (tipo IN ('USUARIO', 'SISTEMA'));

ALTER TABLE carteras ALTER COLUMN usuario_id DROP NOT NULL;

-- La del sistema no tiene dueño; las de usuario sí
ALTER TABLE carteras ADD CONSTRAINT ck_carteras_duenio CHECK (
  (tipo = 'USUARIO' AND usuario_id IS NOT NULL) OR (tipo = 'SISTEMA' AND usuario_id IS NULL));

-- Una sola billetera del sistema
CREATE UNIQUE INDEX ux_carteras_sistema ON carteras (tipo) WHERE tipo = 'SISTEMA';

INSERT INTO carteras (id, usuario_id, saldo, version, tipo)
VALUES ('00000000-0000-0000-0000-000000000001', NULL, 0, 0, 'SISTEMA')
ON CONFLICT DO NOTHING;

-- COBRO: entra a la billetera del sistema el pago de una compra en línea.
-- DEVOLUCION: sale de la billetera del sistema el reembolso de esa compra.
-- UNIQUE(tipo, referencia_id) sigue haciendo idempotente cada uno (uno por venta u orden).
ALTER TABLE movimientos_cartera DROP CONSTRAINT movimientos_cartera_tipo_check;
ALTER TABLE movimientos_cartera ADD CONSTRAINT movimientos_cartera_tipo_check
  CHECK (tipo IN ('RECARGA', 'DEBITO', 'REEMBOLSO', 'COBRO', 'DEVOLUCION'));
