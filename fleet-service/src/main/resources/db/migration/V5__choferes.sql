-- =====================================================================
-- V5 · fleet-service · Choferes (datos laborales y licencia)
-- Publica ChoferCreado / ChoferActualizado / ChoferDesactivado / LicenciaChoferActualizada.
-- El id se genera ANTES de llamar a Identity (R5) y sirve como llave de idempotencia.
-- Foto, correo, teléfono, NIT, DPI y dirección viven en el perfil de Identity
-- (la foto es obligatoria al crear el chofer: se valida aquí y en Identity).
-- =====================================================================

CREATE TABLE choferes (
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

CREATE TRIGGER tr_choferes_fecha
  BEFORE UPDATE ON choferes
  FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();

CREATE TRIGGER tr_choferes_no_eliminar
  BEFORE DELETE ON choferes
  FOR EACH ROW EXECUTE FUNCTION fn_bloquear_eliminacion();
