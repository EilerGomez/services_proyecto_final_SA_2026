-- =====================================================================
-- V3 · fleet-service · Personal de sucursal (admins de sucursal y cajeros)
-- Publica PersonalSucursalAsignado / PersonalSucursalDesactivado.
-- Alta: Fleet genera el id, llama a R5 (POST /identity/interno/cuentas-personal) con ese id
-- como solicitudId y guarda la asignación con el usuario_id devuelto.
-- Traslado: se desactiva la fila actual y se crea otra con el mismo usuario_id.
-- =====================================================================

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

CREATE TRIGGER tr_personal_fecha
  BEFORE UPDATE ON personal_sucursal
  FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();

CREATE TRIGGER tr_personal_no_eliminar
  BEFORE DELETE ON personal_sucursal
  FOR EACH ROW EXECUTE FUNCTION fn_bloquear_eliminacion();

-- Cada sucursal activa debe conservar al menos un ADMIN_SUCURSAL activo.
-- La aplicación lo valida antes (con la sucursal bloqueada); este trigger es la última defensa.
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

CREATE TRIGGER tr_ultimo_admin_sucursal
  BEFORE UPDATE OF activo ON personal_sucursal
  FOR EACH ROW EXECUTE FUNCTION fn_validar_ultimo_admin_sucursal();
