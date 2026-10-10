-- =====================================================================
-- V7 · sales-service · Órdenes de pago de alquileres privados
-- La crea el manejador de CotizacionAlquilerConfirmada (Trips): no hay endpoint para crearla.
-- Pago con CARTERA (S12, saga con Identity; solo si el alquiler tiene cuenta) o EFECTIVO (S13, mostrador).
-- Estados: PENDIENTE_PAGO → PAGADA (CarteraDebitada o S13) | RECHAZADA (DebitoCarteraRechazado; se puede
--          reintentar con S12) · PENDIENTE_PAGO/RECHAZADA → REEMBOLSADA (débito tardío devuelto).
-- Publica (paso 6): DebitoCarteraSolicitado, AlquilerPagado (Trips pasa el alquiler a PAGADO),
--                   ReembolsoCarteraSolicitado (débito tardío).
-- =====================================================================

CREATE TABLE ordenes_alquiler (
  id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  alquiler_id          UUID          NOT NULL UNIQUE,    
  cliente_usuario_id   UUID,                            
  cliente_nit          VARCHAR(20)   NOT NULL DEFAULT 'CF',
  cliente_nombre       VARCHAR(201)  NOT NULL,
  cliente_correo       VARCHAR(150),
  sucursal_id          UUID          NOT NULL,
  origen               VARCHAR(255)  NOT NULL,           
  destino              VARCHAR(255)  NOT NULL,
  monto                NUMERIC(12,2) NOT NULL CHECK (monto > 0),
  metodo_pago          VARCHAR(10)   CHECK (metodo_pago IN ('CARTERA', 'EFECTIVO')),
  fecha_pago           DATE,                             
  estado               VARCHAR(16)   NOT NULL DEFAULT 'PENDIENTE_PAGO'
                       CHECK (estado IN ('PENDIENTE_PAGO', 'PAGADA', 'RECHAZADA', 'REEMBOLSADA')),
  motivo_rechazo       VARCHAR(255),                    
  version              INT           NOT NULL DEFAULT 0,
  fecha_creacion       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  fecha_actualizacion  TIMESTAMPTZ   NOT NULL DEFAULT now(),
  CHECK (estado = 'PENDIENTE_PAGO' OR (metodo_pago IS NOT NULL AND fecha_pago IS NOT NULL)),
  CHECK (estado <> 'RECHAZADA' OR motivo_rechazo IS NOT NULL),
  CHECK (metodo_pago IS DISTINCT FROM 'CARTERA' OR cliente_usuario_id IS NOT NULL)   
);

CREATE INDEX ix_ordenes_nit      ON ordenes_alquiler (cliente_nit);            
CREATE INDEX ix_ordenes_cliente  ON ordenes_alquiler (cliente_usuario_id);     
CREATE INDEX ix_ordenes_sucursal ON ordenes_alquiler (sucursal_id, estado);    

CREATE TRIGGER tr_ordenes_fecha BEFORE UPDATE ON ordenes_alquiler
  FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
