-- =====================================================================
-- V1__esquema_cartera.sql  ·  Wallet Service (cartera digital)
-- Base: bd_cartera  ·  PostgreSQL 13+
-- Antes vivía en bd_identidad. usuario_id es una referencia lógica a Identity (sin FK):
-- cada servicio es dueño de su base.
-- =====================================================================

CREATE OR REPLACE FUNCTION fn_actualizar_fecha() RETURNS trigger AS $$
BEGIN
  NEW.fecha_actualizacion := now();
  RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_bloquear_eliminacion() RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'Los registros de % no se eliminan', TG_TABLE_NAME;
END; $$ LANGUAGE plpgsql;

-- ---------- Cartera (una por usuario) ----------
CREATE TABLE carteras (
  id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  usuario_id           UUID          NOT NULL UNIQUE,      -- referencia lógica a Identity
  saldo                NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (saldo >= 0),
  version              INT           NOT NULL DEFAULT 0,
  fecha_creacion       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  fecha_actualizacion  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- ---------- Movimientos ----------
CREATE TABLE movimientos_cartera (
  id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  cartera_id          UUID          NOT NULL REFERENCES carteras(id),
  tipo                VARCHAR(10)   NOT NULL CHECK (tipo IN ('RECARGA', 'DEBITO', 'REEMBOLSO')),
  monto               NUMERIC(12,2) NOT NULL CHECK (monto > 0),
  saldo_resultante    NUMERIC(12,2) NOT NULL CHECK (saldo_resultante >= 0),
  tipo_referencia     VARCHAR(10)   NOT NULL CHECK (tipo_referencia IN ('VENTA', 'ALQUILER', 'MANUAL')),
  referencia_id       UUID,                         -- venta_id u orden de alquiler (null en recargas)
  proveedor_pago      VARCHAR(30),                  -- recargas: pasarela que cobró (SIMULADO, ...)
  referencia_externa  VARCHAR(100),                 -- recargas: id del cobro en la pasarela
  fecha_movimiento    DATE          NOT NULL,       -- fecha elegida por el usuario
  fecha_creacion      TIMESTAMPTZ   NOT NULL DEFAULT now(),
  -- Idempotencia de la saga: un débito y un reembolso por venta u orden como máximo
  UNIQUE (tipo, referencia_id),
  -- Una recarga por cobro de la pasarela (un webhook repetido no acredita dos veces)
  UNIQUE (proveedor_pago, referencia_externa)
);
CREATE INDEX ix_movimientos_cartera ON movimientos_cartera (cartera_id, fecha_movimiento DESC);

-- ---------- Outbox e idempotencia (módulo comun) ----------
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

-- ---------- Reglas en BD ----------
CREATE TRIGGER tr_carteras_fecha BEFORE UPDATE ON carteras FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();

-- El dinero no se borra: ni carteras ni movimientos
CREATE TRIGGER tr_carteras_no_eliminar BEFORE DELETE ON carteras
  FOR EACH ROW EXECUTE FUNCTION fn_bloquear_eliminacion();
CREATE TRIGGER tr_movimientos_no_eliminar BEFORE DELETE ON movimientos_cartera
  FOR EACH ROW EXECUTE FUNCTION fn_bloquear_eliminacion();
