-- =====================================================================
-- V12 · trips-service · Tablas de eventos que usa el módulo comun
-- eventos_salida: outbox. PublicadorEventos inserta aquí en la misma transacción del cambio;
--                 RelayOutbox lo envía a RabbitMQ (trips.eventos) y marca fecha_publicacion.
-- eventos_procesados: idempotencia. ConsumidorEventos registra cada evento recibido de trips.q
--                     para no procesarlo dos veces.
-- =====================================================================

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
