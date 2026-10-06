-- =====================================================================
-- 04_bd_ventas.sql  ·  Sales Service (boletos, asientos y pagos)
-- Base: bd_ventas  ·  PostgreSQL 13+
-- =====================================================================

CREATE OR REPLACE FUNCTION fn_actualizar_fecha() RETURNS trigger AS $$
BEGIN
  NEW.fecha_actualizacion := now();
  RETURN NEW;
END; $$ LANGUAGE plpgsql;

-- ---------- Proyecciones locales ----------
CREATE TABLE vista_viajes (                -- ViajeProgramado / ViajeActualizado / ViajeIniciado / ViajeEliminado (Trips)
  viaje_id             UUID          PRIMARY KEY,
  ruta_id              UUID          NOT NULL,
  sucursal_origen_id   UUID          NOT NULL,
  origen_nombre        VARCHAR(100)  NOT NULL,
  destino_nombre       VARCHAR(100)  NOT NULL,
  fecha_salida         TIMESTAMPTZ   NOT NULL,
  bus_id               UUID          NOT NULL,
  bus_placa            VARCHAR(10)   NOT NULL,
  capacidad            INT           NOT NULL CHECK (capacidad > 0),
  precio_boleto        NUMERIC(12,2) NOT NULL,
  estado               VARCHAR(12)   NOT NULL CHECK (estado IN ('PROGRAMADO', 'EN_RUTA', 'COMPLETADO')),
  version              INT           NOT NULL DEFAULT 0
);
CREATE INDEX ix_vista_viajes_salida ON vista_viajes (fecha_salida) WHERE estado = 'PROGRAMADO';

CREATE TABLE vista_clientes (              -- ClienteRegistrado / PerfilActualizado (Identity)
  usuario_id  UUID         PRIMARY KEY,
  nit         VARCHAR(20)  NOT NULL UNIQUE,
  nombre      VARCHAR(200) NOT NULL,
  correo      VARCHAR(150) NOT NULL
);

-- ---------- Asientos por viaje ----------
-- Se crean filas 1..capacidad al consumir ViajeProgramado.
CREATE TABLE asientos_viaje (
  viaje_id         UUID         NOT NULL REFERENCES vista_viajes(viaje_id) ON DELETE CASCADE,
  numero_asiento   INT          NOT NULL CHECK (numero_asiento > 0),
  estado           VARCHAR(12)  NOT NULL DEFAULT 'DISPONIBLE'
                   CHECK (estado IN ('DISPONIBLE', 'RESERVADO', 'VENDIDO')),
  venta_id         UUID,
  reservado_hasta  TIMESTAMPTZ,
  version          INT          NOT NULL DEFAULT 0,
  PRIMARY KEY (viaje_id, numero_asiento),
  CHECK (estado = 'DISPONIBLE' OR venta_id IS NOT NULL)
);
-- Bloqueo concurrente (en una transacción):
--   UPDATE asientos_viaje SET estado = 'RESERVADO', venta_id = :venta, reservado_hasta = now() + interval '5 min',
--          version = version + 1
--   WHERE viaje_id = :viaje AND numero_asiento = ANY(:asientos) AND estado = 'DISPONIBLE';
--   -- si filas afectadas <> cantidad de asientos pedidos -> ROLLBACK y responder 409

-- ---------- Ventas y boletos ----------
CREATE TABLE ventas (
  id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  viaje_id             UUID          NOT NULL,
  canal                VARCHAR(10)   NOT NULL CHECK (canal IN ('EN_LINEA', 'PRESENCIAL')),
  cliente_usuario_id   UUID,                         -- null en venta presencial
  cliente_nit          VARCHAR(20)   NOT NULL DEFAULT 'CF',
  cliente_nombre       VARCHAR(200)  NOT NULL DEFAULT 'Consumidor final',
  cliente_correo       VARCHAR(150),
  sucursal_id          UUID          NOT NULL,       -- sucursal a la que se atribuye el ingreso
  cajero_usuario_id    UUID,                         -- quien vendió en mostrador
  metodo_pago          VARCHAR(10)   NOT NULL CHECK (metodo_pago IN ('CARTERA', 'EFECTIVO')),
  monto_total          NUMERIC(12,2) NOT NULL CHECK (monto_total > 0),
  fecha_pago           DATE          NOT NULL,       -- fecha elegida por el usuario
  estado               VARCHAR(16)   NOT NULL DEFAULT 'PENDIENTE_PAGO'
                       CHECK (estado IN ('PENDIENTE_PAGO', 'CONFIRMADA', 'RECHAZADA', 'CANCELADA', 'REEMBOLSADA')),
  motivo_rechazo       VARCHAR(255),
  version              INT           NOT NULL DEFAULT 0,
  fecha_creacion       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  fecha_actualizacion  TIMESTAMPTZ   NOT NULL DEFAULT now(),
  CHECK (canal = 'PRESENCIAL' OR cliente_usuario_id IS NOT NULL),
  CHECK (canal = 'EN_LINEA'   OR cajero_usuario_id  IS NOT NULL)
);
CREATE INDEX ix_ventas_nit     ON ventas (cliente_nit);          -- historial por NIT
CREATE INDEX ix_ventas_viaje   ON ventas (viaje_id);
CREATE INDEX ix_ventas_pend    ON ventas (fecha_creacion) WHERE estado = 'PENDIENTE_PAGO';

CREATE TABLE boletos (
  id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  venta_id         UUID          NOT NULL REFERENCES ventas(id),
  viaje_id         UUID          NOT NULL,
  numero_asiento   INT           NOT NULL CHECK (numero_asiento > 0),
  precio           NUMERIC(12,2) NOT NULL CHECK (precio > 0),
  estado           VARCHAR(10)   NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'CANCELADO'))
);
-- Segunda barrera contra doble venta del mismo asiento
CREATE UNIQUE INDEX ux_boleto_asiento_activo ON boletos (viaje_id, numero_asiento) WHERE estado = 'ACTIVO';

-- ---------- Pago de alquiler privado ----------
CREATE TABLE ordenes_alquiler (
  id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  alquiler_id          UUID          NOT NULL UNIQUE,  -- id del alquiler en Trips (CotizacionAlquilerConfirmada)
  cliente_usuario_id   UUID,
  cliente_nit          VARCHAR(20)   NOT NULL DEFAULT 'CF',
  cliente_nombre       VARCHAR(200)  NOT NULL,
  cliente_correo       VARCHAR(150),
  sucursal_id          UUID          NOT NULL,
  monto                NUMERIC(12,2) NOT NULL CHECK (monto > 0),
  metodo_pago          VARCHAR(10)   CHECK (metodo_pago IN ('CARTERA', 'EFECTIVO')),
  fecha_pago           DATE,
  estado               VARCHAR(16)   NOT NULL DEFAULT 'PENDIENTE_PAGO'
                       CHECK (estado IN ('PENDIENTE_PAGO', 'PAGADA', 'RECHAZADA', 'REEMBOLSADA')),
  version              INT           NOT NULL DEFAULT 0,
  fecha_creacion       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  fecha_actualizacion  TIMESTAMPTZ   NOT NULL DEFAULT now(),
  CHECK (estado = 'PENDIENTE_PAGO' OR (metodo_pago IS NOT NULL AND fecha_pago IS NOT NULL))
);
CREATE INDEX ix_ordenes_nit ON ordenes_alquiler (cliente_nit);

-- ---------- Outbox e idempotencia ----------
CREATE TABLE eventos_salida (
  id                 UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  tipo_agregado      VARCHAR(50)  NOT NULL,
  agregado_id        UUID         NOT NULL,
  tipo_evento        VARCHAR(80)  NOT NULL,
  version_evento     INT          NOT NULL DEFAULT 1,
  datos              JSONB        NOT NULL,
  fecha_creacion     TIMESTAMPTZ  NOT NULL DEFAULT now(),
  fecha_publicacion  TIMESTAMPTZ,
  intentos           INT          NOT NULL DEFAULT 0,
  ultimo_error       TEXT
);
CREATE INDEX ix_eventos_pendientes ON eventos_salida (fecha_creacion) WHERE fecha_publicacion IS NULL;

CREATE TABLE eventos_procesados (
  evento_id         UUID         PRIMARY KEY,
  tipo_evento       VARCHAR(80)  NOT NULL,
  fecha_procesado   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TRIGGER tr_ventas_fecha  BEFORE UPDATE ON ventas           FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
CREATE TRIGGER tr_ordenes_fecha BEFORE UPDATE ON ordenes_alquiler FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
