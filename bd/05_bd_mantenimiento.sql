-- =====================================================================
-- 05_bd_mantenimiento.sql  ·  Maintenance Service (taller y repuestos)
-- Base: bd_mantenimiento  ·  PostgreSQL 13+
-- =====================================================================

CREATE OR REPLACE FUNCTION fn_actualizar_fecha() RETURNS trigger AS $$
BEGIN
  NEW.fecha_actualizacion := now();
  RETURN NEW;
END; $$ LANGUAGE plpgsql;

-- ---------- Proyección local (BusCreated / BusUpdated / BusDeactivated) ----------
CREATE TABLE vista_buses (
  bus_id       UUID        PRIMARY KEY,
  placa        VARCHAR(10) NOT NULL,
  sucursal_id  UUID        NOT NULL,
  activo       BOOLEAN     NOT NULL,
  version      INT         NOT NULL DEFAULT 0
);

-- ---------- Catálogo de categorías ----------
CREATE TABLE categorias_gasto (
  id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  nombre               VARCHAR(60)   NOT NULL UNIQUE,
  descripcion          VARCHAR(255),
  activa               BOOLEAN       NOT NULL DEFAULT TRUE,
  fecha_creacion       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  fecha_actualizacion  TIMESTAMPTZ   NOT NULL DEFAULT now()
);

INSERT INTO categorias_gasto (nombre) VALUES
  ('Llantas'), ('Motor'), ('Frenos'), ('Suspensión'), ('Cambio de aceite'),
  ('Eléctrico'), ('Carrocería'), ('Otros');

-- ---------- Servicios de taller (encabezado + detalle) ----------
CREATE TABLE servicios_taller (
  id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  bus_id               UUID          NOT NULL REFERENCES vista_buses(bus_id),
  sucursal_id          UUID          NOT NULL,
  fecha_servicio       DATE          NOT NULL,       -- fecha elegida por el usuario
  taller               VARCHAR(150),
  observaciones        VARCHAR(500),
  total_mano_obra      NUMERIC(12,2) NOT NULL DEFAULT 0,
  total_repuestos      NUMERIC(12,2) NOT NULL DEFAULT 0,
  total                NUMERIC(12,2) NOT NULL DEFAULT 0,
  registrado_por       UUID          NOT NULL,
  fecha_creacion       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  fecha_actualizacion  TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX ix_servicios_bus   ON servicios_taller (bus_id, fecha_servicio);
CREATE INDEX ix_servicios_fecha ON servicios_taller (sucursal_id, fecha_servicio);

CREATE TABLE detalle_servicio_taller (
  id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  servicio_id   UUID          NOT NULL REFERENCES servicios_taller(id) ON DELETE CASCADE,
  categoria_id  UUID          NOT NULL REFERENCES categorias_gasto(id) ON DELETE RESTRICT,
  descripcion   VARCHAR(255)  NOT NULL,
  tipo          VARCHAR(10)   NOT NULL CHECK (tipo IN ('MANO_OBRA', 'REPUESTO')),
  monto         NUMERIC(12,2) NOT NULL CHECK (monto > 0)
);
CREATE INDEX ix_detalle_categoria ON detalle_servicio_taller (categoria_id);

-- Recalcula los totales del encabezado al cambiar el detalle
CREATE OR REPLACE FUNCTION fn_recalcular_totales_servicio() RETURNS trigger AS $$
DECLARE
  v_servicio UUID := COALESCE(NEW.servicio_id, OLD.servicio_id);
BEGIN
  UPDATE servicios_taller s SET
    total_mano_obra = COALESCE((SELECT SUM(monto) FROM detalle_servicio_taller WHERE servicio_id = v_servicio AND tipo = 'MANO_OBRA'), 0),
    total_repuestos = COALESCE((SELECT SUM(monto) FROM detalle_servicio_taller WHERE servicio_id = v_servicio AND tipo = 'REPUESTO'), 0),
    total           = COALESCE((SELECT SUM(monto) FROM detalle_servicio_taller WHERE servicio_id = v_servicio), 0)
  WHERE s.id = v_servicio;
  RETURN NULL;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER tr_recalcular_totales AFTER INSERT OR UPDATE OR DELETE ON detalle_servicio_taller
  FOR EACH ROW EXECUTE FUNCTION fn_recalcular_totales_servicio();

-- Una categoría con gastos no se elimina (la FK RESTRICT lo impide); solo se pone activa = FALSE.
-- Una categoría inactiva no se puede usar en líneas nuevas.
CREATE OR REPLACE FUNCTION fn_validar_categoria_activa() RETURNS trigger AS $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM categorias_gasto WHERE id = NEW.categoria_id AND activa) THEN
    RAISE EXCEPTION 'La categoría de gasto está desactivada';
  END IF;
  RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER tr_categoria_activa BEFORE INSERT ON detalle_servicio_taller
  FOR EACH ROW EXECUTE FUNCTION fn_validar_categoria_activa();

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

CREATE TRIGGER tr_categorias_fecha BEFORE UPDATE ON categorias_gasto FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
CREATE TRIGGER tr_servicios_fecha  BEFORE UPDATE ON servicios_taller FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
