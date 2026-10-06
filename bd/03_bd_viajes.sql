-- =====================================================================
-- 03_bd_viajes.sql  ·  Trips Service (rutas, viajes, alquileres y costos)
-- Base: bd_viajes  ·  PostgreSQL 13+
-- =====================================================================

CREATE OR REPLACE FUNCTION fn_actualizar_fecha() RETURNS trigger AS $$
BEGIN
  NEW.fecha_actualizacion := now();
  RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION fn_registro_inmutable() RETURNS trigger AS $$
BEGIN
  RAISE EXCEPTION 'Los registros de % no se pueden modificar ni eliminar', TG_TABLE_NAME;
END; $$ LANGUAGE plpgsql;

-- ---------- Proyecciones locales (alimentadas por eventos) ----------
CREATE TABLE vista_sucursales (            -- SucursalCreada / SucursalActualizada (Fleet)
  id        UUID         PRIMARY KEY,
  nombre    VARCHAR(100) NOT NULL,
  latitud   NUMERIC(9,6) NOT NULL,
  longitud  NUMERIC(9,6) NOT NULL,
  activa    BOOLEAN      NOT NULL,
  version   INT          NOT NULL DEFAULT 0
);

CREATE TABLE vista_tarifas_depreciacion (  -- TarifaDepreciacionCambiada (Identity)
  id             UUID          PRIMARY KEY,
  monto_por_km   NUMERIC(12,4) NOT NULL,
  vigente_desde  DATE          NOT NULL UNIQUE
);

CREATE TABLE vista_buses (                 -- BusCreado / BusActualizado / BusDesactivado (Fleet)
  id                   UUID        PRIMARY KEY,
  sucursal_id          UUID        NOT NULL,
  placa                VARCHAR(10) NOT NULL,
  capacidad_pasajeros  INT         NOT NULL,
  estado               VARCHAR(15) NOT NULL,
  version              INT         NOT NULL DEFAULT 0
);

CREATE TABLE vista_choferes (              -- ChoferCreado / ChoferActualizado / ChoferDesactivado (Fleet)
  id                  UUID          PRIMARY KEY,
  sucursal_id         UUID          NOT NULL,
  nombre_completo     VARCHAR(201)  NOT NULL,
  salario_base_viaje  NUMERIC(12,2) NOT NULL,
  estado              VARCHAR(10)   NOT NULL,
  version             INT           NOT NULL DEFAULT 0
);

-- ---------- Configuración propia ----------
CREATE TABLE configuracion_alquiler (
  id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  precio_por_km        NUMERIC(12,2) NOT NULL CHECK (precio_por_km > 0),
  vigente_desde        DATE          NOT NULL UNIQUE,
  creado_por           UUID          NOT NULL,
  fecha_creacion       TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- ---------- Rutas ----------
CREATE TABLE rutas (
  id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  sucursal_origen_id   UUID          NOT NULL,
  sucursal_destino_id  UUID          NOT NULL,
  distancia_km         NUMERIC(8,2)  NOT NULL CHECK (distancia_km > 0),
  precio_boleto        NUMERIC(12,2) NOT NULL CHECK (precio_boleto > 0),
  activa               BOOLEAN       NOT NULL DEFAULT TRUE,
  creado_por           UUID          NOT NULL,     -- admin de la sucursal de origen
  fecha_creacion       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  fecha_actualizacion  TIMESTAMPTZ   NOT NULL DEFAULT now(),
  CHECK (sucursal_origen_id <> sucursal_destino_id),
  UNIQUE (sucursal_origen_id, sucursal_destino_id)
);

-- ---------- Alquileres privados ----------
CREATE TABLE alquileres_privados (
  id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  cliente_usuario_id   UUID,                       -- null si se solicitó en mostrador sin cuenta
  cliente_nit          VARCHAR(20)   NOT NULL DEFAULT 'CF',
  cliente_nombre       VARCHAR(150)  NOT NULL,
  cliente_correo       VARCHAR(150),
  sucursal_id          UUID          NOT NULL,     -- sucursal que cotiza y atiende
  canal                VARCHAR(10)   NOT NULL CHECK (canal IN ('EN_LINEA', 'PRESENCIAL')),
  origen               VARCHAR(255)  NOT NULL,
  destino              VARCHAR(255)  NOT NULL,
  fecha_salida         TIMESTAMPTZ   NOT NULL,
  fecha_retorno        TIMESTAMPTZ,                -- null si es solo ida
  cantidad_pasajeros   INT           NOT NULL CHECK (cantidad_pasajeros > 0),
  km_estimados         NUMERIC(8,2)  NOT NULL CHECK (km_estimados > 0),
  precio_estimado      NUMERIC(12,2) NOT NULL CHECK (precio_estimado > 0),
  precio_confirmado    NUMERIC(12,2) CHECK (precio_confirmado > 0),
  confirmado_por       UUID,
  estado               VARCHAR(12)   NOT NULL DEFAULT 'SOLICITADO'
                       CHECK (estado IN ('SOLICITADO', 'COTIZADO', 'PAGADO', 'ASIGNADO', 'COMPLETADO', 'CANCELADO')),
  fecha_solicitud      DATE          NOT NULL,     -- fecha elegida por el usuario
  fecha_creacion       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  fecha_actualizacion  TIMESTAMPTZ   NOT NULL DEFAULT now(),
  CHECK (fecha_retorno IS NULL OR fecha_retorno >= fecha_salida),
  CHECK (estado IN ('SOLICITADO', 'CANCELADO') OR precio_confirmado IS NOT NULL)
);
CREATE INDEX ix_alquileres_sucursal ON alquileres_privados (sucursal_id, estado);
CREATE INDEX ix_alquileres_nit ON alquileres_privados (cliente_nit);

-- ---------- Viajes ----------
CREATE TABLE viajes (
  id                        UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  tipo                      VARCHAR(10)   NOT NULL CHECK (tipo IN ('REGULAR', 'PRIVADO')),
  ruta_id                   UUID          REFERENCES rutas(id) ON DELETE RESTRICT,
  alquiler_id               UUID          UNIQUE REFERENCES alquileres_privados(id),
  sucursal_id               UUID          NOT NULL,
  bus_id                    UUID          NOT NULL,
  chofer_id                 UUID          NOT NULL,
  capacidad_bus             INT           NOT NULL CHECK (capacidad_bus > 0),  -- snapshot
  precio_boleto             NUMERIC(12,2),                                     -- snapshot (solo REGULAR)
  salario_base_chofer       NUMERIC(12,2) NOT NULL,                            -- snapshot de Fleet (R1)
  fecha_salida_programada   TIMESTAMPTZ   NOT NULL,
  fecha_llegada_estimada    TIMESTAMPTZ   NOT NULL,
  estado                    VARCHAR(12)   NOT NULL DEFAULT 'PROGRAMADO'
                            CHECK (estado IN ('PROGRAMADO', 'EN_RUTA', 'COMPLETADO')),
  version                   INT           NOT NULL DEFAULT 0,
  creado_por                UUID          NOT NULL,
  fecha_creacion            TIMESTAMPTZ   NOT NULL DEFAULT now(),
  fecha_actualizacion       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  CHECK (fecha_llegada_estimada > fecha_salida_programada),
  CHECK ( (tipo = 'REGULAR' AND ruta_id IS NOT NULL AND alquiler_id IS NULL AND precio_boleto IS NOT NULL)
       OR (tipo = 'PRIVADO' AND alquiler_id IS NOT NULL AND ruta_id IS NULL) )
);
CREATE INDEX ix_viajes_bus    ON viajes (bus_id, estado);
CREATE INDEX ix_viajes_chofer ON viajes (chofer_id, estado);
CREATE INDEX ix_viajes_ruta   ON viajes (ruta_id, fecha_salida_programada);
-- Eliminar viaje: solo si estado = 'PROGRAMADO'. Se borra físicamente y se publica
-- ViajeEliminado; así una ruta queda libre para eliminarse cuando ya no tenga viajes.

-- El tipo de viaje no se puede cambiar
CREATE OR REPLACE FUNCTION fn_tipo_viaje_inmutable() RETURNS trigger AS $$
BEGIN
  IF NEW.tipo <> OLD.tipo THEN
    RAISE EXCEPTION 'El tipo de viaje no se puede modificar';
  END IF;
  RETURN NEW;
END; $$ LANGUAGE plpgsql;
CREATE TRIGGER tr_tipo_viaje_inmutable BEFORE UPDATE OF tipo ON viajes
  FOR EACH ROW EXECUTE FUNCTION fn_tipo_viaje_inmutable();

-- Solo se eliminan viajes que no han iniciado
CREATE OR REPLACE FUNCTION fn_eliminar_solo_programado() RETURNS trigger AS $$
BEGIN
  IF OLD.estado <> 'PROGRAMADO' THEN
    RAISE EXCEPTION 'Solo se pueden eliminar viajes que no han iniciado';
  END IF;
  RETURN OLD;
END; $$ LANGUAGE plpgsql;
CREATE TRIGGER tr_eliminar_solo_programado BEFORE DELETE ON viajes
  FOR EACH ROW EXECUTE FUNCTION fn_eliminar_solo_programado();

-- ---------- Salida y llegada (inmutables) ----------
CREATE TABLE salidas_viaje (
  viaje_id           UUID          PRIMARY KEY REFERENCES viajes(id) ON DELETE RESTRICT,
  bus_id             UUID          NOT NULL,
  chofer_id          UUID          NOT NULL,
  fecha_hora_salida  TIMESTAMPTZ   NOT NULL,       -- fecha elegida por el usuario
  km_inicial         NUMERIC(12,2) NOT NULL CHECK (km_inicial >= 0),
  registrado_por     UUID          NOT NULL,
  fecha_registro     TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE llegadas_viaje (
  viaje_id              UUID          PRIMARY KEY REFERENCES salidas_viaje(viaje_id) ON DELETE RESTRICT,
  fecha_hora_llegada    TIMESTAMPTZ   NOT NULL,
  km_final              NUMERIC(12,2) NOT NULL,
  km_recorridos         NUMERIC(12,2) NOT NULL CHECK (km_recorridos >= 0),
  costo_combustible     NUMERIC(12,2) NOT NULL CHECK (costo_combustible >= 0),
  tarifa_depreciacion   NUMERIC(12,4) NOT NULL,    -- snapshot de la tarifa vigente
  costo_depreciacion    NUMERIC(12,2) NOT NULL,    -- km_recorridos * tarifa
  salario_chofer        NUMERIC(12,2) NOT NULL,    -- base, o base * 1.15 si es PRIVADO
  registrado_por        UUID          NOT NULL,
  fecha_registro        TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TRIGGER tr_salidas_inmutables  BEFORE UPDATE OR DELETE ON salidas_viaje
  FOR EACH ROW EXECUTE FUNCTION fn_registro_inmutable();
CREATE TRIGGER tr_llegadas_inmutables BEFORE UPDATE OR DELETE ON llegadas_viaje
  FOR EACH ROW EXECUTE FUNCTION fn_registro_inmutable();

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

CREATE TRIGGER tr_rutas_fecha       BEFORE UPDATE ON rutas               FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
CREATE TRIGGER tr_alquileres_fecha  BEFORE UPDATE ON alquileres_privados FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
CREATE TRIGGER tr_viajes_fecha      BEFORE UPDATE ON viajes              FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
