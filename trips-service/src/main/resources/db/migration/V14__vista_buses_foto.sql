-- =====================================================================
-- V14 · trips-service · Foto del bus en la copia local
-- El pasajero compra el boleto sin sesión, así que no puede pedirle el bus a Fleet (sus datos de
-- flota son de administración). La foto viaja en BusCreado / BusActualizado y se guarda aquí, para
-- mostrarle con qué bus viaja y que lo reconozca por el color en la terminal.
-- Es nullable: los buses que ya estaban copiados se quedan sin foto hasta su próximo evento.
-- =====================================================================

ALTER TABLE vista_buses ADD COLUMN foto_url VARCHAR(500);
