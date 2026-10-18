-- Migración Flyway V5 · reporting-service
-- RS4 (ingresos por alquiler) muestra las fechas de salida y retorno del
-- alquiler, además de la fecha de pago. Las trae el evento AlquilerPagado.

ALTER TABLE ingresos
  ADD COLUMN fecha_salida  TIMESTAMPTZ,
  ADD COLUMN fecha_retorno TIMESTAMPTZ;
