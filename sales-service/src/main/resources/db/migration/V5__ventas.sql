-- =====================================================================
-- V5 · sales-service · Ventas de boletos
-- EN_LINEA (S2): cliente con cuenta, paga con CARTERA (saga con Identity), empieza PENDIENTE_PAGO.
-- PRESENCIAL (S4): cajero, paga en EFECTIVO, nace CONFIRMADA.
-- Estados: PENDIENTE_PAGO → CONFIRMADA (CarteraDebitada) | RECHAZADA (saldo insuficiente, tiempo agotado,
--          viaje eliminado) · CONFIRMADA → CANCELADA (S5 o ViajeEliminado) → REEMBOLSADA (CarteraReembolsada)
--          · RECHAZADA → REEMBOLSADA (débito tardío devuelto).
-- Reglas (CHECK con nombre): en línea paga con CARTERA y mostrador con EFECTIVO; RECHAZADA lleva motivo;
--   CANCELADA lleva motivo y fecha de cancelación.
-- Publica (paso 6): DebitoCarteraSolicitado, BoletoVendido, VentaCancelada, ReembolsoCarteraSolicitado.
-- =====================================================================

CREATE TABLE ventas (
  id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  viaje_id             UUID          NOT NULL,           -- sin FK: el viaje se puede borrar de vista_viajes
  canal                VARCHAR(10)   NOT NULL CHECK (canal IN ('EN_LINEA', 'PRESENCIAL')),
  cliente_usuario_id   UUID,                             -- null en venta presencial
  cliente_nit          VARCHAR(20)   NOT NULL DEFAULT 'CF',
  cliente_nombre       VARCHAR(201)  NOT NULL DEFAULT 'Consumidor final',
  cliente_correo       VARCHAR(150),
  sucursal_id          UUID          NOT NULL,           -- sucursal de origen del viaje (ingreso)
  cajero_usuario_id    UUID,                             -- quien vendió en mostrador
  metodo_pago          VARCHAR(10)   NOT NULL CHECK (metodo_pago IN ('CARTERA', 'EFECTIVO')),
  monto_total          NUMERIC(12,2) NOT NULL CHECK (monto_total > 0),
  fecha_pago           DATE          NOT NULL,           -- fecha elegida por el usuario
  estado               VARCHAR(16)   NOT NULL DEFAULT 'PENDIENTE_PAGO'
                       CHECK (estado IN ('PENDIENTE_PAGO', 'CONFIRMADA', 'RECHAZADA', 'CANCELADA', 'REEMBOLSADA')),
  motivo_rechazo       VARCHAR(255),                     -- SALDO_INSUFICIENTE, TIEMPO_AGOTADO, VIAJE_ELIMINADO...
  motivo_cancelacion   VARCHAR(255),                     -- S5 (lo escribe el admin) o el motivo de ViajeEliminado
  fecha_cancelacion    DATE,                             -- S5: elegida por el usuario. ViajeEliminado: hoy
  version              INT           NOT NULL DEFAULT 0,
  fecha_creacion       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  fecha_actualizacion  TIMESTAMPTZ   NOT NULL DEFAULT now(),
  CONSTRAINT ck_ventas_cliente_en_linea CHECK (canal = 'PRESENCIAL' OR cliente_usuario_id IS NOT NULL),
  CONSTRAINT ck_ventas_cajero_mostrador CHECK (canal = 'EN_LINEA' OR cajero_usuario_id IS NOT NULL),
  CONSTRAINT ck_ventas_metodo_pago CHECK (
    (canal = 'EN_LINEA' AND metodo_pago = 'CARTERA') OR (canal = 'PRESENCIAL' AND metodo_pago = 'EFECTIVO')),
  CONSTRAINT ck_ventas_motivo_rechazo CHECK (estado <> 'RECHAZADA' OR motivo_rechazo IS NOT NULL),
  CONSTRAINT ck_ventas_cancelacion CHECK (
    estado <> 'CANCELADA' OR (motivo_cancelacion IS NOT NULL AND fecha_cancelacion IS NOT NULL))
);

CREATE INDEX ix_ventas_nit        ON ventas (cliente_nit);                      -- historial por NIT (S7, S8)
CREATE INDEX ix_ventas_viaje      ON ventas (viaje_id);                         -- ViajeEliminado
CREATE INDEX ix_ventas_sucursal   ON ventas (sucursal_id, fecha_pago);          -- S6
CREATE INDEX ix_ventas_pendientes ON ventas (fecha_creacion) WHERE estado = 'PENDIENTE_PAGO';

CREATE TRIGGER tr_ventas_fecha BEFORE UPDATE ON ventas
  FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();