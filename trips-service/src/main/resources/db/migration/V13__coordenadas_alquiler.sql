-- =====================================================================
-- V13 · trips-service · Coordenadas del recorrido de un alquiler
-- El cliente marca el origen y el destino en el mapa al solicitar el bus.
-- Son opcionales (los alquileres anteriores no las tienen), pero si un
-- punto trae coordenadas deben venir latitud y longitud juntas.
-- =====================================================================

ALTER TABLE alquileres_privados
  ADD COLUMN origen_latitud    NUMERIC(9, 6),
  ADD COLUMN origen_longitud   NUMERIC(9, 6),
  ADD COLUMN destino_latitud   NUMERIC(9, 6),
  ADD COLUMN destino_longitud  NUMERIC(9, 6);

ALTER TABLE alquileres_privados
  ADD CONSTRAINT ck_alquiler_origen_coordenadas
    CHECK ((origen_latitud IS NULL) = (origen_longitud IS NULL)),
  ADD CONSTRAINT ck_alquiler_destino_coordenadas
    CHECK ((destino_latitud IS NULL) = (destino_longitud IS NULL)),
  ADD CONSTRAINT ck_alquiler_origen_latitud
    CHECK (origen_latitud BETWEEN -90 AND 90),
  ADD CONSTRAINT ck_alquiler_origen_longitud
    CHECK (origen_longitud BETWEEN -180 AND 180),
  ADD CONSTRAINT ck_alquiler_destino_latitud
    CHECK (destino_latitud BETWEEN -90 AND 90),
  ADD CONSTRAINT ck_alquiler_destino_longitud
    CHECK (destino_longitud BETWEEN -180 AND 180);

COMMENT ON COLUMN alquileres_privados.origen_latitud IS 'Latitud del origen marcada por el cliente (opcional).';
COMMENT ON COLUMN alquileres_privados.origen_longitud IS 'Longitud del origen marcada por el cliente (opcional).';
COMMENT ON COLUMN alquileres_privados.destino_latitud IS 'Latitud del destino marcada por el cliente (opcional).';
COMMENT ON COLUMN alquileres_privados.destino_longitud IS 'Longitud del destino marcada por el cliente (opcional).';
