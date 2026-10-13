-- Migración Flyway V4 · reporting-service
-- Hasta la V3 la columna version de buses y choferes era @Version de JPA y subía también con
-- los cambios locales (total_viajes y chofer_actual_id), así que puede ser mayor que la versión
-- real de Fleet y descartar sus eventos. Desde ahora guarda solo la aggregateVersion de Fleet:
-- se reinicia en 0 para que el siguiente evento de Fleet la vuelva a fijar.
-- Sucursales y viajes no se tocan (solo cambiaban con eventos, nunca quedaban por encima).

UPDATE buses SET version = 0;
UPDATE choferes SET version = 0;
