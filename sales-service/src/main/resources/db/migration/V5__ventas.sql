-- =====================================================================
-- V5 · sales-service · Ventas de boletos
-- EN_LINEA (S2): cliente con cuenta, paga con CARTERA (saga con Identity), empieza PENDIENTE_PAGO.
-- PRESENCIAL (S4): cajero, paga en EFECTIVO, nace CONFIRMADA.
-- Estados: PENDIENTE_PAGO → CONFIRMADA (CarteraDebitada) | RECHAZADA (saldo insuficiente, tiempo agotado,
--          viaje eliminado) · CONFIRMADA → CANCELADA (S5 o ViajeEliminado) → REEMBOLSADA (CarteraReembolsada)
--          · RECHAZADA → REEMBOLSADA (débito tardío devuelto).
-- Publica (paso 6): DebitoCarteraSolicitado, BoletoVendido, VentaCancelada, ReembolsoCarteraSolicitado.
-- =====================================================================

CREATE TABLE ventas (
  id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  viaje_id             UUID          NOT NULL,           
  canal                VARCHAR(10)   NOT NULL CHECK (canal IN ('EN_LINEA', 'PRESENCIAL')),
  cliente_usuario_id   UUID,                             
  cliente_nit          VARCHAR(20)   NOT NULL DEFAULT 'CF',
  cliente_nombre       VARCHAR(201)  NOT NULL DEFAULT 'Consumidor final',
  cliente_correo       VARCHAR(150),
  sucursal_id          UUID          NOT NULL,           
  cajero_usuario_id    UUID,                             
  metodo_pago          VARCHAR(10)   NOT NULL CHECK (metodo_pago IN ('CARTERA', 'EFECTIVO')),
  monto_total          NUMERIC(12,2) NOT NULL CHECK (monto_total > 0),
  fecha_pago           DATE          NOT NULL,           
  estado               VARCHAR(16)   NOT NULL DEFAULT 'PENDIENTE_PAGO'
                       CHECK (estado IN ('PENDIENTE_PAGO', 'CONFIRMADA', 'RECHAZADA', 'CANCELADA', 'REEMBOLSADA')),
  motivo_rechazo       VARCHAR(255),                     
  motivo_cancelacion   VARCHAR(255),                     
  fecha_cancelacion    DATE,                             -
  version              INT           NOT NULL DEFAULT 0,
  fecha_creacion       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  fecha_actualizacion  TIMESTAMPTZ   NOT NULL DEFAULT now(),
  CHECK (canal = 'PRESENCIAL' OR cliente_usuario_id IS NOT NULL),
  CHECK (canal = 'EN_LINEA'   OR cajero_usuario_id  IS NOT NULL),
  CHECK ((canal = 'EN_LINEA') = (metodo_pago = 'CARTERA')),          
  CHECK (estado <> 'RECHAZADA' OR motivo_rechazo IS NOT NULL),
  CHECK (estado <> 'CANCELADA' OR (motivo_cancelacion IS NOT NULL AND fecha_cancelacion IS NOT NULL))
);

CREATE INDEX ix_ventas_nit        ON ventas (cliente_nit);                      -- historial por NIT (S7, S8)
CREATE INDEX ix_ventas_viaje      ON ventas (viaje_id);                         -- ViajeEliminado
CREATE INDEX ix_ventas_sucursal   ON ventas (sucursal_id, fecha_pago);          -- S6
CREATE INDEX ix_ventas_pendientes ON ventas (fecha_creacion) WHERE estado = 'PENDIENTE_PAGO';

CREATE TRIGGER tr_ventas_fecha BEFORE UPDATE ON ventas
  FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
