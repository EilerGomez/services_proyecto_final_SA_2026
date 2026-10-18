-- =====================================================================
-- 06_bd_reportes.sql  ·  Reporting Service (modelo de lectura)
-- Base: bd_reportes  ·  PostgreSQL 13+
-- Todas las tablas se llenan SOLO con eventos. Sin FKs a propósito:
-- los eventos pueden llegar en distinto orden.
-- =====================================================================

CREATE TABLE sucursales (                  -- SucursalCreada / SucursalActualizada (Fleet)
  id         UUID         PRIMARY KEY,
  nombre     VARCHAR(100) NOT NULL,
  latitud    NUMERIC(9,6) NOT NULL,
  longitud   NUMERIC(9,6) NOT NULL,
  activa     BOOLEAN      NOT NULL,
  version    INT          NOT NULL DEFAULT 0
);

CREATE TABLE tarifas_depreciacion (        -- TarifaDepreciacionCambiada (Identity)
  id             UUID          PRIMARY KEY,
  monto_por_km   NUMERIC(12,4) NOT NULL,
  vigente_desde  DATE          NOT NULL UNIQUE
);
-- Solo para mostrar la tarifa vigente en el reporte de depreciación.
-- El costo de cada viaje usa la tarifa que guardó Trips en ViajeCompletado (viajes.tarifa_depreciacion).
-- Tarifa vigente para una fecha:
--   SELECT monto_por_km, vigente_desde FROM tarifas_depreciacion
--   WHERE vigente_desde <= :fecha ORDER BY vigente_desde DESC LIMIT 1;

CREATE TABLE rutas (                       -- RutaCreada / RutaActualizada / RutaEliminada (Trips)
  id                   UUID          PRIMARY KEY,
  sucursal_origen_id   UUID          NOT NULL,
  sucursal_destino_id  UUID          NOT NULL,
  origen_nombre        VARCHAR(100)  NOT NULL,
  destino_nombre       VARCHAR(100)  NOT NULL,
  distancia_km         NUMERIC(8,2)  NOT NULL,
  precio_boleto        NUMERIC(12,2) NOT NULL,
  activa               BOOLEAN       NOT NULL DEFAULT TRUE,
  version              INT           NOT NULL DEFAULT 0
);

CREATE TABLE buses (                       -- BusCreado / BusActualizado / BusDesactivado (Fleet)
  id                   UUID          PRIMARY KEY,
  sucursal_id          UUID          NOT NULL,
  placa                VARCHAR(10)   NOT NULL,
  marca                VARCHAR(50)   NOT NULL,
  modelo               VARCHAR(50)   NOT NULL,
  capacidad_pasajeros  INT           NOT NULL,
  estado               VARCHAR(15)   NOT NULL,
  kilometraje_actual   NUMERIC(12,2) NOT NULL DEFAULT 0,
  chofer_actual_id     UUID,          -- chofer del viaje en curso o del próximo programado
  total_viajes         INT           NOT NULL DEFAULT 0,
  version              INT           NOT NULL DEFAULT 0
);

CREATE TABLE choferes (                    -- ChoferCreado / ChoferActualizado / ChoferDesactivado (Fleet)
  id                          UUID         PRIMARY KEY,
  sucursal_id                 UUID         NOT NULL,
  nombre_completo             VARCHAR(201) NOT NULL,
  numero_licencia             VARCHAR(30)  NOT NULL,
  tipo_licencia               VARCHAR(2)   NOT NULL,
  fecha_vencimiento_licencia  DATE         NOT NULL,
  estado                      VARCHAR(10)  NOT NULL,
  total_viajes                INT          NOT NULL DEFAULT 0,
  version                     INT          NOT NULL DEFAULT 0
);

CREATE TABLE viajes (                      -- ViajeProgramado / ViajeActualizado / ViajeEliminado / ViajeIniciado / ViajeCompletado (Trips)
  viaje_id               UUID          PRIMARY KEY,
  tipo                   VARCHAR(10)   NOT NULL,
  ruta_id                UUID,
  sucursal_id            UUID          NOT NULL,
  bus_id                 UUID          NOT NULL,
  chofer_id              UUID          NOT NULL,
  fecha_salida           TIMESTAMPTZ   NOT NULL,
  km_recorridos          NUMERIC(12,2),
  tarifa_depreciacion    NUMERIC(12,4),
  costo_depreciacion     NUMERIC(12,2),
  estado                 VARCHAR(12)   NOT NULL,
  version                INT           NOT NULL DEFAULT 0
);
CREATE INDEX ix_viajes_bus ON viajes (bus_id) WHERE estado = 'COMPLETADO';
CREATE INDEX ix_viajes_bus_pendientes ON viajes (bus_id, fecha_salida) WHERE estado IN ('PROGRAMADO', 'EN_RUTA');

-- Ingresos: boletos y alquileres (monto negativo en reembolsos/cancelaciones)
-- BoletoVendido / VentaCancelada / AlquilerPagado (Sales)
CREATE TABLE ingresos (
  id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  evento_origen_id  UUID          NOT NULL UNIQUE,   -- idempotencia
  sucursal_id       UUID          NOT NULL,
  tipo              VARCHAR(10)   NOT NULL CHECK (tipo IN ('BOLETO', 'ALQUILER')),
  referencia_id     UUID          NOT NULL,          -- venta_id u orden de alquiler
  viaje_id          UUID,
  ruta_id           UUID,
  bus_id            UUID,
  cliente_nit       VARCHAR(20),
  cliente_nombre    VARCHAR(200),
  origen            VARCHAR(255),
  destino           VARCHAR(255),
  fecha_salida      TIMESTAMPTZ,                      -- RS4: salida del alquiler
  fecha_retorno     TIMESTAMPTZ,                      -- RS4: retorno del alquiler
  cantidad_boletos  INT           NOT NULL DEFAULT 0,
  monto             NUMERIC(12,2) NOT NULL,
  fecha_negocio     DATE          NOT NULL            -- fecha elegida por el usuario
);
CREATE INDEX ix_ingresos_fecha      ON ingresos (fecha_negocio, sucursal_id);
CREATE INDEX ix_ingresos_ruta       ON ingresos (ruta_id) WHERE tipo = 'BOLETO';
CREATE INDEX ix_ingresos_referencia ON ingresos (referencia_id);

-- Gastos: combustible, depreciación, salario, mano de obra y repuestos
-- ViajeCompletado (Trips) / GastoTallerRegistrado (Maintenance)
CREATE TABLE gastos (
  id                  UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  evento_origen_id    UUID          NOT NULL,
  sucursal_id         UUID          NOT NULL,
  tipo                VARCHAR(15)   NOT NULL
                      CHECK (tipo IN ('COMBUSTIBLE', 'DEPRECIACION', 'SALARIO_CHOFER', 'MANO_OBRA', 'REPUESTO')),
  bus_id              UUID          NOT NULL,
  viaje_id            UUID,
  servicio_taller_id  UUID,
  linea_detalle_id    UUID,
  categoria_id        UUID,
  categoria_nombre    VARCHAR(60),
  descripcion         VARCHAR(255),
  monto               NUMERIC(12,2) NOT NULL,
  fecha_negocio       DATE          NOT NULL
);
-- Idempotencia: un ViajeCompletado genera 3 filas (combustible, depreciación, salario);
-- un servicio de taller genera una fila por línea de detalle.
CREATE UNIQUE INDEX ux_gastos_evento ON gastos
  (evento_origen_id, tipo, COALESCE(linea_detalle_id, '00000000-0000-0000-0000-000000000000'::uuid));
CREATE INDEX ix_gastos_fecha     ON gastos (fecha_negocio, sucursal_id);
CREATE INDEX ix_gastos_categoria ON gastos (categoria_id, bus_id) WHERE categoria_id IS NOT NULL;

CREATE TABLE eventos_procesados (
  evento_id         UUID         PRIMARY KEY,
  tipo_evento       VARCHAR(80)  NOT NULL,
  fecha_procesado   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ---------- Ejemplo: reporte de ganancias por sucursal ----------
-- WITH i AS (SELECT sucursal_id, SUM(monto) ingresos FROM ingresos
--            WHERE (:desde IS NULL OR fecha_negocio >= :desde) AND (:hasta IS NULL OR fecha_negocio <= :hasta)
--            GROUP BY sucursal_id),
--      g AS (SELECT sucursal_id, SUM(monto) gastos FROM gastos
--            WHERE (:desde IS NULL OR fecha_negocio >= :desde) AND (:hasta IS NULL OR fecha_negocio <= :hasta)
--            GROUP BY sucursal_id)
-- SELECT s.nombre, COALESCE(i.ingresos,0) ingresos, COALESCE(g.gastos,0) gastos,
--        COALESCE(i.ingresos,0) - COALESCE(g.gastos,0) ganancia
-- FROM sucursales s LEFT JOIN i ON i.sucursal_id = s.id LEFT JOIN g ON g.sucursal_id = s.id
-- ORDER BY s.nombre;
