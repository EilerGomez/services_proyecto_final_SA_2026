-- =====================================================================
-- V2 · sales-service · Copia local de los viajes REGULAR de Trips
-- La escriben solo los manejadores de ViajeProgramado, ViajeActualizado, ViajeIniciado y ViajeEliminado
-- (upsert con version <=). Los viajes PRIVADO no se venden y no se copian.
-- S1 y la venta (S2, S4) leen de aquí: origen, destino, salida, placa, capacidad y precio del boleto.
-- =====================================================================

CREATE TABLE vista_viajes (
  viaje_id             UUID          PRIMARY KEY,                
  ruta_id              UUID          NOT NULL,
  sucursal_origen_id   UUID          NOT NULL,                   
  origen_nombre        VARCHAR(100)  NOT NULL,
  destino_nombre       VARCHAR(100)  NOT NULL,
  fecha_salida         TIMESTAMPTZ   NOT NULL,
  bus_id               UUID          NOT NULL,
  bus_placa            VARCHAR(10),                              
  capacidad            INT           NOT NULL CHECK (capacidad > 0),
  precio_boleto        NUMERIC(12,2) NOT NULL CHECK (precio_boleto > 0),
  estado               VARCHAR(12)   NOT NULL CHECK (estado IN ('PROGRAMADO', 'EN_RUTA')),
  version              INT           NOT NULL DEFAULT 0          
);

CREATE INDEX ix_vista_viajes_salida ON vista_viajes (fecha_salida) WHERE estado = 'PROGRAMADO';
