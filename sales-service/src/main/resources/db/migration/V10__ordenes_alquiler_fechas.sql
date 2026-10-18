-- Migración Flyway V10 · sales-service
-- Las órdenes de alquiler guardan las fechas de salida y retorno (las trae
-- CotizacionAlquilerConfirmada de Trips) para que el reporte de ingresos por
-- alquiler pueda mostrar el intervalo del alquiler y no solo la fecha de pago.

ALTER TABLE ordenes_alquiler
  ADD COLUMN fecha_salida  TIMESTAMPTZ,
  ADD COLUMN fecha_retorno TIMESTAMPTZ;
