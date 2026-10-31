-- =====================================================================
-- V19 · trips-service · Kilometraje del bus y cuenta del piloto en las copias de Fleet
--
-- kilometraje_actual: Fleet lo sube con cada viaje completado y lo manda en BusCreado/BusActualizado.
--   Trips lo necesita para que la salida no se registre con un kilometraje menor al que ya trae el
--   bus: el odómetro no camina para atrás.
--
-- usuario_id: la cuenta del piloto en Identity. Con ella el aviso de "se te asignó un viaje" puede
--   salir a su correo sin que Trips guarde correos (los resuelve Notification contra Identity).
--
-- Las dos columnas llegan por evento, así que las filas que ya están se quedan con el valor por
-- defecto hasta que Fleet vuelva a publicar ese bus o ese chofer.
-- =====================================================================

ALTER TABLE vista_buses ADD COLUMN kilometraje_actual NUMERIC(12,2) NOT NULL DEFAULT 0
  CHECK (kilometraje_actual >= 0);

ALTER TABLE vista_choferes ADD COLUMN usuario_id UUID;
