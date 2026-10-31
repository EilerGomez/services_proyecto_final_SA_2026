-- =====================================================================
-- V18 · trips-service · El regreso sabe cuál es su ida
--
-- viaje_ida_id enlaza el viaje de vuelta con el de ida que le dio origen. Sirve para dos reglas:
--   1. El regreso no puede salir mientras su ida no esté COMPLETADA: es el mismo bus y el mismo
--      recorrido, así que físicamente no puede arrancar la vuelta antes de llegar.
--   2. El regreso viaja en el mismo bus que la ida (el bus que se fue es el que se regresa).
--
-- ON DELETE SET NULL: si se elimina la ida (se cancela la salida), el regreso queda suelto y puede
-- operarse por su cuenta, en vez de impedir el borrado con un error de llave foránea.
-- =====================================================================

ALTER TABLE viajes ADD COLUMN viaje_ida_id UUID REFERENCES viajes (id) ON DELETE SET NULL;

-- Solo un viaje de regreso tiene ida; y nunca puede ser él mismo.
ALTER TABLE viajes ADD CONSTRAINT ck_viajes_ida_solo_regreso CHECK (
  viaje_ida_id IS NULL OR (sentido = 'REGRESO' AND viaje_ida_id <> id));

CREATE INDEX ix_viajes_ida ON viajes (viaje_ida_id) WHERE viaje_ida_id IS NOT NULL;

-- Los regresos que ya existían (programados antes de esta migración) se enlazan con la ida más
-- reciente de su misma ruta que salga antes que ellos, que es justo como se crean: ida y vuelta
-- juntas, en ese orden. Si no aparece ninguna, el regreso queda suelto y opera sin la regla.
UPDATE viajes r
   SET viaje_ida_id = (
       SELECT i.id
         FROM viajes i
        WHERE i.ruta_id = r.ruta_id
          AND i.sentido = 'IDA'
          AND i.tipo = 'REGULAR'
          AND i.fecha_salida_programada <= r.fecha_salida_programada
        ORDER BY i.fecha_salida_programada DESC
        LIMIT 1)
 WHERE r.sentido = 'REGRESO'
   AND r.viaje_ida_id IS NULL;
