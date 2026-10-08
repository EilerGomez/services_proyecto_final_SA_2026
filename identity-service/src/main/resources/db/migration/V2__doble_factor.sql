-- Migración Flyway V2 · identity-service: doble factor por correo (OTP).
-- Una vez aplicada NO se modifica: los cambios van en V3__...

ALTER TABLE usuarios
  ADD COLUMN doble_factor_activo BOOLEAN NOT NULL DEFAULT FALSE;

-- Códigos de un solo uso (se guarda el hash SHA-256, nunca el código).
CREATE TABLE codigos_doble_factor (
  id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  usuario_id      UUID         NOT NULL REFERENCES usuarios(id),
  codigo_hash     VARCHAR(255) NOT NULL,
  expira_en       TIMESTAMPTZ  NOT NULL,
  usado_en        TIMESTAMPTZ,
  intentos        INT          NOT NULL DEFAULT 0 CHECK (intentos >= 0),
  fecha_creacion  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX ix_codigos_doble_factor_usuario ON codigos_doble_factor (usuario_id);
