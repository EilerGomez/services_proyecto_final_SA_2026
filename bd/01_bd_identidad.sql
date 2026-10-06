-- =====================================================================
-- 01_bd_identidad.sql  ·  Identity Service (cuentas, roles, perfiles, tarifa y cartera)
-- Base: bd_identidad  ·  PostgreSQL 13+
-- Las sucursales y su personal viven en Fleet (bd_flota). Aquí solo se guarda
-- usuarios.sucursal_id para el claim "sucursalId" del JWT y para R4.
-- =====================================================================

-- ---------- Funciones utilitarias ----------
CREATE OR REPLACE FUNCTION fn_actualizar_fecha() RETURNS trigger AS $$
BEGIN
  NEW.fecha_actualizacion := now();
  RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_bloquear_eliminacion() RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'Los registros de % no se eliminan, solo se desactivan', TG_TABLE_NAME;
END; $$ LANGUAGE plpgsql;

-- ---------- Roles ----------
CREATE TABLE roles (
  codigo       VARCHAR(30)  PRIMARY KEY,
  nombre       VARCHAR(60)  NOT NULL,
  descripcion  VARCHAR(255)
);

INSERT INTO roles (codigo, nombre, descripcion) VALUES
  ('ADMIN_SISTEMA',  'Administrador del sistema',  'Sucursales, administradores de sucursal, configuración global y reportes globales'),
  ('ADMIN_SUCURSAL', 'Administrador de sucursal',  'Buses, choferes, cajeros, rutas, viajes, alquileres, taller y reportes de su sucursal'),
  ('CAJERO',         'Cajero',                     'Venta presencial de boletos y alquileres en su sucursal'),
  ('CHOFER',         'Chofer',                     'Consulta sus viajes y registra salida y llegada'),
  ('CLIENTE',        'Cliente',                    'Compra en línea, cartera digital, alquileres e historial');

-- ---------- Usuarios y perfiles ----------
CREATE TABLE usuarios (
  id                   UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  correo               VARCHAR(150) NOT NULL UNIQUE,
  contrasena_hash      VARCHAR(255) NOT NULL,
  estado               VARCHAR(15)  NOT NULL DEFAULT 'ACTIVO'
                       CHECK (estado IN ('ACTIVO', 'INACTIVO', 'BLOQUEADO')),
  intentos_fallidos    INT          NOT NULL DEFAULT 0 CHECK (intentos_fallidos >= 0),
  bloqueado_hasta      TIMESTAMPTZ,
  chofer_id            UUID         UNIQUE,          -- referencia lógica a Fleet (sin FK)
  sucursal_id          UUID,                         -- referencia lógica a Fleet (sin FK); null para clientes y ADMIN_SISTEMA
  ultimo_acceso        TIMESTAMPTZ,
  fecha_creacion       TIMESTAMPTZ  NOT NULL DEFAULT now(),
  fecha_actualizacion  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- usuarios.sucursal_id se mantiene así:
--   R5 (POST /identity/interno/cuentas-personal) -> la asigna al crear la cuenta
--   PersonalSucursalAsignado (Fleet)      -> la cambia en un traslado y agrega el rol
--   PersonalSucursalDesactivado (Fleet)   -> la limpia y quita ADMIN_SUCURSAL/CAJERO
--   ChoferDesactivado (Fleet)             -> la limpia y quita CHOFER
-- Un cambio de sucursal se refleja en el JWT en el siguiente inicio de sesión.

CREATE TABLE usuario_roles (
  usuario_id        UUID        NOT NULL REFERENCES usuarios(id),
  rol_codigo        VARCHAR(30) NOT NULL REFERENCES roles(codigo),
  fecha_asignacion  TIMESTAMPTZ NOT NULL DEFAULT now(),
  PRIMARY KEY (usuario_id, rol_codigo)
);

CREATE TABLE perfiles (
  usuario_id           UUID         PRIMARY KEY REFERENCES usuarios(id),
  nombres              VARCHAR(100) NOT NULL,
  apellidos            VARCHAR(100) NOT NULL,
  nit                  VARCHAR(20)  NOT NULL UNIQUE,
  dpi                  VARCHAR(13)  NOT NULL UNIQUE CHECK (dpi ~ '^[0-9]{13}$'),
  telefono             VARCHAR(20)  NOT NULL,
  direccion            VARCHAR(255) NOT NULL,
  foto_url             VARCHAR(500),               -- opcional para todos; obligatoria para choferes
  fecha_actualizacion  TIMESTAMPTZ  NOT NULL DEFAULT now()
);
-- Regla del chofer: un usuario con chofer_id debe tener foto.
-- Se valida en R5 y además con el trigger tr_foto_chofer.

CREATE TABLE tokens_refresco (
  id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  usuario_id      UUID         NOT NULL REFERENCES usuarios(id),
  token_hash      VARCHAR(255) NOT NULL UNIQUE,
  expira_en       TIMESTAMPTZ  NOT NULL,
  revocado_en     TIMESTAMPTZ,
  fecha_creacion  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE tokens_recuperacion (
  id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
  usuario_id      UUID         NOT NULL REFERENCES usuarios(id),
  token_hash      VARCHAR(255) NOT NULL UNIQUE,
  expira_en       TIMESTAMPTZ  NOT NULL,
  usado_en        TIMESTAMPTZ,
  fecha_creacion  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------- Idempotencia de R5 (Fleet -> Identity) ----------
-- Fleet genera el id del chofer o del personal ANTES de llamar a R5 y lo envía
-- como solicitudId. Si la llamada se reintenta, Identity devuelve la misma cuenta.
CREATE TABLE solicitudes_cuenta_personal (
  solicitud_id    UUID        PRIMARY KEY,           -- choferId o personalId generado en Fleet
  usuario_id      UUID        NOT NULL REFERENCES usuarios(id),
  rol_codigo      VARCHAR(30) NOT NULL REFERENCES roles(codigo)
                  CHECK (rol_codigo IN ('CHOFER', 'ADMIN_SUCURSAL', 'CAJERO')),
  sucursal_id     UUID        NOT NULL,
  fecha_creacion  TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ---------- Configuración global ----------
CREATE TABLE tarifas_depreciacion (
  id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  monto_por_km    NUMERIC(12,4) NOT NULL CHECK (monto_por_km > 0),
  vigente_desde   DATE          NOT NULL UNIQUE,
  creado_por      UUID          NOT NULL REFERENCES usuarios(id),
  fecha_creacion  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
-- Tarifa vigente para una fecha:
--   SELECT monto_por_km FROM tarifas_depreciacion
--   WHERE vigente_desde <= :fecha ORDER BY vigente_desde DESC LIMIT 1;

-- ---------- Cartera digital ----------
CREATE TABLE carteras (
  id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  usuario_id           UUID          NOT NULL UNIQUE REFERENCES usuarios(id),
  saldo                NUMERIC(12,2) NOT NULL DEFAULT 0 CHECK (saldo >= 0),
  version              INT           NOT NULL DEFAULT 0,
  fecha_creacion       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  fecha_actualizacion  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE movimientos_cartera (
  id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  cartera_id        UUID          NOT NULL REFERENCES carteras(id),
  tipo              VARCHAR(10)   NOT NULL CHECK (tipo IN ('RECARGA', 'DEBITO', 'REEMBOLSO')),
  monto             NUMERIC(12,2) NOT NULL CHECK (monto > 0),
  saldo_resultante  NUMERIC(12,2) NOT NULL CHECK (saldo_resultante >= 0),
  tipo_referencia   VARCHAR(10)   NOT NULL CHECK (tipo_referencia IN ('VENTA', 'ALQUILER', 'MANUAL')),
  referencia_id     UUID,                         -- venta_id u orden de alquiler (null en recargas)
  fecha_movimiento  DATE          NOT NULL,       -- fecha elegida por el usuario
  fecha_creacion    TIMESTAMPTZ   NOT NULL DEFAULT now(),
  -- Idempotencia de la saga: un débito y un reembolso por venta como máximo
  UNIQUE (tipo, referencia_id)
);
CREATE INDEX ix_movimientos_cartera ON movimientos_cartera (cartera_id, fecha_movimiento DESC);

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
CREATE TRIGGER tr_usuarios_fecha BEFORE UPDATE ON usuarios FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
CREATE TRIGGER tr_perfiles_fecha BEFORE UPDATE ON perfiles FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
CREATE TRIGGER tr_carteras_fecha BEFORE UPDATE ON carteras FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();

-- Los usuarios no se eliminan
CREATE TRIGGER tr_usuarios_no_eliminar BEFORE DELETE ON usuarios FOR EACH ROW EXECUTE FUNCTION fn_bloquear_eliminacion();

-- Siempre debe existir al menos un ADMIN_SISTEMA activo.
-- (La aplicación además debe bloquear con SELECT ... FOR UPDATE para evitar carreras.)
CREATE OR REPLACE FUNCTION fn_validar_ultimo_admin_sistema() RETURNS trigger AS $$
BEGIN
  IF OLD.estado = 'ACTIVO' AND NEW.estado <> 'ACTIVO'
     AND EXISTS (SELECT 1 FROM usuario_roles WHERE usuario_id = OLD.id AND rol_codigo = 'ADMIN_SISTEMA')
     AND NOT EXISTS (SELECT 1 FROM usuarios u
                     JOIN usuario_roles ur ON ur.usuario_id = u.id
                     WHERE ur.rol_codigo = 'ADMIN_SISTEMA' AND u.estado = 'ACTIVO' AND u.id <> OLD.id) THEN
    RAISE EXCEPTION 'No se puede desactivar al último administrador del sistema activo';
  END IF;
  RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER tr_ultimo_admin_sistema BEFORE UPDATE OF estado ON usuarios
  FOR EACH ROW EXECUTE FUNCTION fn_validar_ultimo_admin_sistema();

CREATE OR REPLACE FUNCTION fn_validar_foto_chofer() RETURNS trigger AS $$
BEGIN
  IF NEW.foto_url IS NULL OR btrim(NEW.foto_url) = '' THEN
    IF EXISTS (SELECT 1 FROM usuarios WHERE id = NEW.usuario_id AND chofer_id IS NOT NULL) THEN
      RAISE EXCEPTION 'La foto es obligatoria para los choferes';
    END IF;
  END IF;
  RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER tr_foto_chofer BEFORE INSERT OR UPDATE OF foto_url ON perfiles
  FOR EACH ROW EXECUTE FUNCTION fn_validar_foto_chofer();

-- ---------- Índices de consulta ----------
CREATE INDEX ix_usuarios_sucursal ON usuarios (sucursal_id) WHERE sucursal_id IS NOT NULL;  -- R4: admins de una sucursal
CREATE INDEX ix_tokens_refresco_usuario ON tokens_refresco (usuario_id);

-- ---------- Datos iniciales ----------
-- Crear el primer ADMIN_SISTEMA desde la aplicación (seeder) para generar
-- el hash con la misma librería (bcrypt/argon2) que usa el login.