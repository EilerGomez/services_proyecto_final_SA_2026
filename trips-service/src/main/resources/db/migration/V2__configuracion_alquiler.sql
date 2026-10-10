-- =====================================================================
-- V2 · trips-service · Configuración del alquiler privado
-- Historial del precio por km (T1, T2). Para cotizar un alquiler (T16) se usa el precio
-- con la vigente_desde más reciente que no pase de la fecha de solicitud.
-- Es un historial: un precio nuevo se registra con otra fecha, los anteriores no cambian.
-- =====================================================================

CREATE TABLE configuracion_alquiler (
  id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  precio_por_km    NUMERIC(12,2) NOT NULL CHECK (precio_por_km > 0),
  vigente_desde    DATE          NOT NULL UNIQUE,     -- un solo precio por fecha
  creado_por       UUID          NOT NULL,            -- usuarioId del ADMIN_SISTEMA (sin FK: vive en Identity)
  fecha_creacion   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TRIGGER tr_configuracion_alquiler_inmutable
  BEFORE UPDATE OR DELETE ON configuracion_alquiler
  FOR EACH ROW EXECUTE FUNCTION fn_registro_inmutable();
