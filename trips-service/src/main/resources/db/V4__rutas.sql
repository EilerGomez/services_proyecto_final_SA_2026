-- =====================================================================
-- V4 · trips-service · Rutas
-- Una ruta une dos sucursales distintas; el par origen-destino es único (el regreso es otra ruta).
-- Las sucursales no tienen FK: viven en Fleet y aquí se validan con vista_sucursales.
-- Se elimina (T7) solo si no tiene viajes: la FK de viajes (V7) es ON DELETE RESTRICT.
-- Publica RutaCreada / RutaActualizada / RutaEliminada.
-- =====================================================================

CREATE TABLE rutas (
  id                   UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  sucursal_origen_id   UUID          NOT NULL,
  sucursal_destino_id  UUID          NOT NULL,
  distancia_km         NUMERIC(8,2)  NOT NULL CHECK (distancia_km > 0),
  precio_boleto        NUMERIC(12,2) NOT NULL CHECK (precio_boleto > 0),   -- precio actual; cada viaje guarda su copia
  activa               BOOLEAN       NOT NULL DEFAULT TRUE,
  version              INT           NOT NULL DEFAULT 0,
  creado_por           UUID          NOT NULL,                             -- usuarioId del admin de la sucursal de origen
  fecha_creacion       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  fecha_actualizacion  TIMESTAMPTZ   NOT NULL DEFAULT now(),
  CHECK (sucursal_origen_id <> sucursal_destino_id),
  UNIQUE (sucursal_origen_id, sucursal_destino_id)
);

-- El UNIQUE ya indexa por (origen, destino): sirve también para filtrar por sucursal de origen (T3)

CREATE TRIGGER tr_rutas_fecha
  BEFORE UPDATE ON rutas
  FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();
