-- =====================================================================
-- V12 · sales-service · Recorrido del alquiler en la orden de pago
-- La orden ya guardaba el origen y el destino como texto, que a veces es el nombre del lugar y a
-- veces las coordenadas tal cual (cuando el geocodificador no encontró nombre). Con los puntos del
-- mapa, la ventanilla ve el recorrido dibujado antes de cobrar en lugar de leer números.
-- Vienen en CotizacionAlquilerConfirmada; son nullables porque las solicitudes viejas no los tienen.
-- =====================================================================

ALTER TABLE ordenes_alquiler ADD COLUMN origen_latitud   NUMERIC(9,6);
ALTER TABLE ordenes_alquiler ADD COLUMN origen_longitud  NUMERIC(9,6);
ALTER TABLE ordenes_alquiler ADD COLUMN destino_latitud  NUMERIC(9,6);
ALTER TABLE ordenes_alquiler ADD COLUMN destino_longitud NUMERIC(9,6);

-- Latitud y longitud van juntas o no van (el mapa necesita el punto completo)
ALTER TABLE ordenes_alquiler ADD CONSTRAINT ck_ordenes_origen_punto CHECK (
  (origen_latitud IS NULL) = (origen_longitud IS NULL));
ALTER TABLE ordenes_alquiler ADD CONSTRAINT ck_ordenes_destino_punto CHECK (
  (destino_latitud IS NULL) = (destino_longitud IS NULL));
