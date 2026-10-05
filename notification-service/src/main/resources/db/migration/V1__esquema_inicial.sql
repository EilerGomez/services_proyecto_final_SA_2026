-- Migración Flyway V1 · notification-service
-- Una vez aplicada NO se modifica: los cambios van en V2__..., V3__...

CREATE TABLE notificaciones (
  id               UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  evento_id        UUID         NOT NULL,       -- evento que la originó
  canal            VARCHAR(10)  NOT NULL DEFAULT 'CORREO' CHECK (canal IN ('CORREO', 'SMS', 'PUSH')),
  destinatario     VARCHAR(150) NOT NULL,
  plantilla        VARCHAR(50)  NOT NULL,       -- COMPRA_BOLETO, ALQUILER_PAGADO, LICENCIA_POR_VENCER, ...
  asunto           VARCHAR(200) NOT NULL,
  datos            JSONB        NOT NULL,
  estado           VARCHAR(10)  NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'ENVIADA', 'FALLIDA')),
  intentos         INT          NOT NULL DEFAULT 0,
  ultimo_error     TEXT,
  fecha_envio      TIMESTAMPTZ,
  fecha_creacion   TIMESTAMPTZ  NOT NULL DEFAULT now(),
  UNIQUE (evento_id, destinatario, plantilla)   -- evita correos duplicados
);
CREATE INDEX ix_notificaciones_pendientes ON notificaciones (fecha_creacion) WHERE estado = 'PENDIENTE';

CREATE TABLE recordatorios_licencia (
  id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  chofer_id           UUID         NOT NULL,
  usuario_id          UUID         NOT NULL,     -- correo y nombre se piden a Identity al enviar (R4)
  sucursal_id         UUID         NOT NULL,
  fecha_vencimiento   DATE         NOT NULL,
  dias_antes          INT          NOT NULL CHECK (dias_antes IN (30, 15, 3)),
  fecha_programada    DATE         NOT NULL,     -- fecha_vencimiento - dias_antes
  estado              VARCHAR(10)  NOT NULL DEFAULT 'PENDIENTE' CHECK (estado IN ('PENDIENTE', 'ENVIADO', 'CANCELADO')),
  fecha_envio         TIMESTAMPTZ,
  fecha_creacion      TIMESTAMPTZ  NOT NULL DEFAULT now(),
  UNIQUE (chofer_id, fecha_vencimiento, dias_antes),
  CHECK (fecha_programada = fecha_vencimiento - dias_antes)
);
CREATE INDEX ix_recordatorios_pendientes ON recordatorios_licencia (fecha_programada) WHERE estado = 'PENDIENTE';
-- Al consumir DriverLicenseUpdated:
--   UPDATE recordatorios_licencia SET estado = 'CANCELADO' WHERE chofer_id = :chofer AND estado = 'PENDIENTE';
--   INSERT 3 filas (30, 15, 3) con la nueva fecha_vencimiento.
-- Job diario (o POST /notifications/reminders/run?date=YYYY-MM-DD para calificar):
--   SELECT * FROM recordatorios_licencia WHERE estado = 'PENDIENTE' AND fecha_programada <= :fecha;

CREATE TABLE eventos_procesados (
  evento_id         UUID         PRIMARY KEY,
  tipo_evento       VARCHAR(80)  NOT NULL,
  fecha_procesado   TIMESTAMPTZ  NOT NULL DEFAULT now()
);
