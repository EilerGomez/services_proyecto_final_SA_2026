-- Migración Flyway V6 · reporting-service
-- RS4 (ingresos por alquiler) debe mostrar el bus asignado, como pide el enunciado.
--
-- El bus no viene en AlquilerPagado: cuando el cliente paga todavía no hay unidad asignada.
-- Se asigna después, con el viaje PRIVADO (T22), así que la placa se resuelve al generar el
-- reporte: ingreso -> alquiler -> viaje de ese alquiler -> bus. Para eso hacen falta las dos
-- columnas de abajo, que es lo único que faltaba para unir las tres tablas.
--
-- Quedan NULL en lo ya guardado: los alquileres viejos saldrán sin placa hasta que su viaje
-- vuelva a publicarse. No se rellenan acá porque Reporting no tiene de dónde sacarlas.

ALTER TABLE viajes
  ADD COLUMN alquiler_id UUID;

ALTER TABLE ingresos
  ADD COLUMN alquiler_id UUID;

-- Buscar el viaje de un alquiler (RS4) y los ingresos de un alquiler
CREATE INDEX ix_viajes_alquiler   ON viajes (alquiler_id)   WHERE alquiler_id IS NOT NULL;
CREATE INDEX ix_ingresos_alquiler ON ingresos (alquiler_id) WHERE alquiler_id IS NOT NULL;

COMMENT ON COLUMN viajes.alquiler_id IS 'Alquiler del que salió el viaje PRIVADO (null en un REGULAR).';
COMMENT ON COLUMN ingresos.alquiler_id IS 'Alquiler cobrado (solo ingresos de tipo ALQUILER).';
