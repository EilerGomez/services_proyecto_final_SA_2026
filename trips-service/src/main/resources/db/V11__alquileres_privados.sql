-- =====================================================================
-- V11 · trips-service · Alquileres privados
-- Ciclo: SOLICITADO (T16) → COTIZADO (T20) → PAGADO (evento AlquilerPagado de Sales)
--        → ASIGNADO (T22, se crea el viaje PRIVADO) → COMPLETADO (llegada del viaje, T15).
--        SOLICITADO → CANCELADO (T21). Si se elimina el viaje PRIVADO (T12), vuelve a PAGADO.
-- EN_LINEA: lo pide un CLIENTE con su cuenta. PRESENCIAL: lo registra un CAJERO (cliente sin cuenta).
-- Publica CotizacionAlquilerConfirmada. También agrega la FK viajes.alquiler_id (pendiente desde V7).
-- =====================================================================

CREATE TABLE alquileres_privados (
  id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  sucursal_id          UUID          NOT NULL,              -- sucursal que cotiza y atiende
  canal                VARCHAR(10)   NOT NULL CHECK (canal IN ('EN_LINEA', 'PRESENCIAL')),
  cliente_usuario_id   UUID,                                -- null si es PRESENCIAL
  cliente_nit          VARCHAR(20)   NOT NULL DEFAULT 'CF',
  cliente_nombre       VARCHAR(150)  NOT NULL,
  cliente_correo       VARCHAR(150),
  origen               VARCHAR(255)  NOT NULL,
  destino              VARCHAR(255)  NOT NULL,
  fecha_salida         TIMESTAMPTZ   NOT NULL,              -- fechas elegidas por el cliente
  fecha_retorno        TIMESTAMPTZ,                         -- null si es solo ida
  cantidad_pasajeros   INT           NOT NULL CHECK (cantidad_pasajeros > 0),
  km_estimados         NUMERIC(8,2)  NOT NULL CHECK (km_estimados > 0),
  precio_estimado      NUMERIC(12,2) NOT NULL CHECK (precio_estimado > 0),   -- km × precio por km vigente
  precio_confirmado    NUMERIC(12,2) CHECK (precio_confirmado > 0),          -- lo fija el admin en T20
  confirmado_por       UUID,
  estado               VARCHAR(12)   NOT NULL DEFAULT 'SOLICITADO'
                       CHECK (estado IN ('SOLICITADO', 'COTIZADO', 'PAGADO', 'ASIGNADO', 'COMPLETADO', 'CANCELADO')),
  motivo_cancelacion   VARCHAR(255),
  fecha_solicitud      DATE          NOT NULL,              -- fecha elegida por el usuario
  solicitado_por       UUID          NOT NULL,              -- usuarioId del cliente o del cajero
  version              INT           NOT NULL DEFAULT 0,
  fecha_creacion       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  fecha_actualizacion  TIMESTAMPTZ   NOT NULL DEFAULT now(),
  CHECK (fecha_retorno IS NULL OR fecha_retorno >= fecha_salida),
  CHECK (canal = 'PRESENCIAL' OR cliente_usuario_id IS NOT NULL),
  CHECK (estado IN ('SOLICITADO', 'CANCELADO') OR precio_confirmado IS NOT NULL),
  CHECK (estado <> 'CANCELADO' OR motivo_cancelacion IS NOT NULL)
);

-- T17 (por sucursal y estado), T18 (los del cliente) e historial por NIT
CREATE INDEX ix_alquileres_sucursal ON alquileres_privados (sucursal_id, estado);
CREATE INDEX ix_alquileres_cliente  ON alquileres_privados (cliente_usuario_id);
CREATE INDEX ix_alquileres_nit      ON alquileres_privados (cliente_nit);

CREATE TRIGGER tr_alquileres_fecha
  BEFORE UPDATE ON alquileres_privados
  FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();

-- El viaje PRIVADO apunta a su alquiler (la columna existe desde V7; UNIQUE: un viaje por alquiler)
ALTER TABLE viajes
  ADD CONSTRAINT fk_viajes_alquiler FOREIGN KEY (alquiler_id) REFERENCES alquileres_privados(id);
