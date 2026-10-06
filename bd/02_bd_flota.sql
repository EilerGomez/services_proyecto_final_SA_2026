-- =====================================================================
-- 02_bd_flota.sql  ·  Fleet Service (sucursales, personal, buses y choferes)
-- Base: bd_flota  ·  PostgreSQL 13+
-- Las cuentas (correo, contraseña, perfil, foto, roles) viven en Identity.
-- Fleet guarda la parte organizativa y laboral y crea las cuentas con R5.
-- =====================================================================

CREATE OR REPLACE FUNCTION fn_actualizar_fecha() RETURNS trigger AS $$
BEGIN
  NEW.fecha_actualizacion := now();
  RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_bloquear_eliminacion() RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'Los registros de % no se eliminan, solo se desactivan', TG_TABLE_NAME;
END; $$ LANGUAGE plpgsql;

-- ---------- Sucursales (publica SucursalCreada / SucursalActualizada) ----------
CREATE TABLE sucursales (
  id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  codigo               VARCHAR(10)  NOT NULL UNIQUE,
  nombre               VARCHAR(100) NOT NULL UNIQUE,
  departamento         VARCHAR(60)  NOT NULL,
  municipio            VARCHAR(60)  NOT NULL,
  direccion            VARCHAR(255) NOT NULL,
  telefono             VARCHAR(20),
  latitud              NUMERIC(9,6) NOT NULL CHECK (latitud  BETWEEN -90  AND 90),
  longitud             NUMERIC(9,6) NOT NULL CHECK (longitud BETWEEN -180 AND 180),
  activa               BOOLEAN      NOT NULL DEFAULT TRUE,
  version              INT          NOT NULL DEFAULT 0,
  fecha_creacion       TIMESTAMPTZ  NOT NULL DEFAULT now(),
  fecha_actualizacion  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------- Personal de sucursal (publica PersonalSucursalAsignado / PersonalSucursalDesactivado) ----------
-- Alta: Fleet genera el id, llama a R5 (POST /identity/cuentas-personal) con ese id
-- como solicitudId y guarda la asignación con el usuario_id devuelto.
-- Traslado: se desactiva la fila actual y se crea otra con el mismo usuario_id.
CREATE TABLE personal_sucursal (
  id                   UUID         PRIMARY KEY,           -- se genera antes de R5 (llave de idempotencia)
  sucursal_id          UUID         NOT NULL REFERENCES sucursales(id),
  usuario_id           UUID         NOT NULL,              -- cuenta en Identity (sin FK)
  nombre_completo      VARCHAR(201) NOT NULL,              -- copia del perfil (se refresca con PerfilActualizado)
  correo               VARCHAR(150) NOT NULL,              -- copia del perfil
  rol_codigo           VARCHAR(30)  NOT NULL CHECK (rol_codigo IN ('ADMIN_SUCURSAL', 'CAJERO')),
  activo               BOOLEAN      NOT NULL DEFAULT TRUE,
  fecha_asignacion     DATE         NOT NULL DEFAULT CURRENT_DATE,
  fecha_desasignacion  DATE,
  version              INT          NOT NULL DEFAULT 0,
  fecha_creacion       TIMESTAMPTZ  NOT NULL DEFAULT now(),
  fecha_actualizacion  TIMESTAMPTZ  NOT NULL DEFAULT now(),
  CHECK (fecha_desasignacion IS NULL OR fecha_desasignacion >= fecha_asignacion)
);
-- Una persona trabaja en una sola sucursal a la vez (el JWT lleva un solo sucursalId)
CREATE UNIQUE INDEX ux_personal_activo ON personal_sucursal (usuario_id) WHERE activo;
CREATE INDEX ix_personal_sucursal ON personal_sucursal (sucursal_id, rol_codigo) WHERE activo;

-- ---------- Buses ----------
CREATE TABLE buses (
  id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  sucursal_id          UUID          NOT NULL REFERENCES sucursales(id),
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
  sucursal_id                 UUID          NOT NULL REFERENCES sucursales(id),
  usuario_id                  UUID          NOT NULL UNIQUE,  -- cuenta CHOFER devuelta por Identity
  nombre_completo             VARCHAR(201)  NOT NULL,         -- copia del perfil (se refresca con PerfilActualizado)
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

-- ---------- Kilometraje por viaje (evento ViajeCompletado) ----------
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
CREATE TRIGGER tr_sucursales_fecha BEFORE UPDATE ON sucursales        FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
CREATE TRIGGER tr_personal_fecha   BEFORE UPDATE ON personal_sucursal FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
CREATE TRIGGER tr_buses_fecha      BEFORE UPDATE ON buses             FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
CREATE TRIGGER tr_choferes_fecha   BEFORE UPDATE ON choferes          FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();

-- Sucursales, personal, buses y choferes no se eliminan, solo se desactivan
CREATE TRIGGER tr_sucursales_no_eliminar BEFORE DELETE ON sucursales        FOR EACH ROW EXECUTE FUNCTION fn_bloquear_eliminacion();
CREATE TRIGGER tr_personal_no_eliminar   BEFORE DELETE ON personal_sucursal FOR EACH ROW EXECUTE FUNCTION fn_bloquear_eliminacion();
CREATE TRIGGER tr_buses_no_eliminar      BEFORE DELETE ON buses             FOR EACH ROW EXECUTE FUNCTION fn_bloquear_eliminacion();
CREATE TRIGGER tr_choferes_no_eliminar   BEFORE DELETE ON choferes          FOR EACH ROW EXECUTE FUNCTION fn_bloquear_eliminacion();

-- Cada sucursal activa debe conservar al menos un ADMIN_SUCURSAL activo.
-- (La aplicación además debe bloquear con SELECT ... FOR UPDATE para evitar carreras.)
CREATE OR REPLACE FUNCTION fn_validar_ultimo_admin_sucursal() RETURNS trigger AS $$
BEGIN
  IF OLD.activo AND NOT NEW.activo AND OLD.rol_codigo = 'ADMIN_SUCURSAL'
     AND NOT EXISTS (SELECT 1 FROM personal_sucursal
                     WHERE sucursal_id = OLD.sucursal_id AND rol_codigo = 'ADMIN_SUCURSAL'
                       AND activo AND id <> OLD.id)
     AND EXISTS (SELECT 1 FROM sucursales WHERE id = OLD.sucursal_id AND activa) THEN
    RAISE EXCEPTION 'La sucursal debe tener al menos un administrador activo';
  END IF;
  RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER tr_ultimo_admin_sucursal BEFORE UPDATE OF activo ON personal_sucursal
  FOR EACH ROW EXECUTE FUNCTION fn_validar_ultimo_admin_sucursal();

-- Nota: "desactivar bus solo sin viajes programados o en tránsito" se valida en la
-- aplicación con la llamada R3 (Fleet -> Trips) antes de poner estado = 'INACTIVO'.
