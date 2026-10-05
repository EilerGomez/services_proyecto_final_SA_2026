-- Migración Flyway V1 · fleet-service
-- Una vez aplicada NO se modifica: los cambios van en V2__..., V3__...

CREATE OR REPLACE FUNCTION fn_actualizar_fecha() RETURNS trigger AS $$
BEGIN
  NEW.fecha_actualizacion := now();
  RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_bloquear_eliminacion() RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'Los registros de % no se eliminan, solo se desactivan', TG_TABLE_NAME;
END; $$ LANGUAGE plpgsql;

-- ---------- Proyección local (evento BranchCreated/BranchUpdated) ----------
CREATE TABLE vista_sucursales (
  id                   UUID         PRIMARY KEY,
  nombre               VARCHAR(100) NOT NULL,
  activa               BOOLEAN      NOT NULL,
  version              INT          NOT NULL DEFAULT 0,
  fecha_actualizacion  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------- Buses ----------
CREATE TABLE buses (
  id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  sucursal_id          UUID          NOT NULL,
  placa                VARCHAR(10)   NOT NULL UNIQUE,
  marca                VARCHAR(50)   NOT NULL,
  modelo               VARCHAR(50)   NOT NULL,
  anio_fabricacion     INT           NOT NULL CHECK (anio_fabricacion BETWEEN 1950 AND 2100),
  capacidad_pasajeros  INT           NOT NULL CHECK (capacidad_pasajeros BETWEEN 1 AND 120),
  estado               VARCHAR(15)   NOT NULL DEFAULT 'ACTIVO'
                       CHECK (estado IN ('ACTIVO', 'EN_RUTA', 'EN_TALLER', 'INACTIVO')),
  kilometraje_actual   NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (kilometraje_actual >= 0),
  foto_url             VARCHAR(500),
  version              INT           NOT NULL DEFAULT 0,
  fecha_creacion       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  fecha_actualizacion  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX ix_buses_sucursal ON buses (sucursal_id, estado);

-- ---------- Choferes ----------
CREATE TABLE choferes (
  -- El id se genera ANTES de llamar a Identity (R5) y sirve como llave de idempotencia.
  -- Foto, correo, teléfono, NIT, DPI y dirección viven en el perfil de Identity
  -- (la foto es obligatoria al crear el chofer: se valida en este servicio y en Identity).
  id                          UUID          PRIMARY KEY,
  sucursal_id                 UUID          NOT NULL,
  usuario_id                  UUID          NOT NULL UNIQUE,  -- cuenta CHOFER devuelta por Identity
  nombre_completo             VARCHAR(150)  NOT NULL,         -- copia del perfil (se refresca con ProfileUpdated)
  numero_licencia             VARCHAR(30)   NOT NULL UNIQUE,
  tipo_licencia               VARCHAR(2)    NOT NULL CHECK (tipo_licencia IN ('A', 'B', 'C', 'M', 'E')),
  fecha_vencimiento_licencia  DATE          NOT NULL,
  salario_base_viaje          NUMERIC(12,2) NOT NULL CHECK (salario_base_viaje > 0),
  estado                      VARCHAR(10)   NOT NULL DEFAULT 'ACTIVO' CHECK (estado IN ('ACTIVO', 'INACTIVO')),
  version                     INT           NOT NULL DEFAULT 0,
  fecha_creacion              TIMESTAMPTZ   NOT NULL DEFAULT now(),
  fecha_actualizacion         TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX ix_choferes_sucursal ON choferes (sucursal_id, estado);
CREATE INDEX ix_choferes_vencimiento ON choferes (fecha_vencimiento_licencia) WHERE estado = 'ACTIVO';

-- ---------- Kilometraje por viaje (evento TripCompleted) ----------
CREATE TABLE bitacora_kilometraje (
  id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  bus_id           UUID          NOT NULL REFERENCES buses(id),
  viaje_id         UUID          NOT NULL UNIQUE,   -- el mismo viaje no suma dos veces
  km_inicial       NUMERIC(12,2) NOT NULL CHECK (km_inicial >= 0),
  km_final         NUMERIC(12,2) NOT NULL,
  fecha_llegada    TIMESTAMPTZ   NOT NULL,
  fecha_registro   TIMESTAMPTZ   NOT NULL DEFAULT now(),
  CHECK (km_final >= km_inicial)
);

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

-- ---------- Reglas en BD ----------
CREATE TRIGGER tr_buses_fecha    BEFORE UPDATE ON buses    FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
CREATE TRIGGER tr_choferes_fecha BEFORE UPDATE ON choferes FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
CREATE TRIGGER tr_buses_no_eliminar    BEFORE DELETE ON buses    FOR EACH ROW EXECUTE FUNCTION fn_bloquear_eliminacion();
CREATE TRIGGER tr_choferes_no_eliminar BEFORE DELETE ON choferes FOR EACH ROW EXECUTE FUNCTION fn_bloquear_eliminacion();
-- Nota: "desactivar solo sin viajes programados o en tránsito" se valida en la
-- aplicación con la llamada R3 (Fleet -> Trips) antes de poner estado = 'INACTIVO'.
