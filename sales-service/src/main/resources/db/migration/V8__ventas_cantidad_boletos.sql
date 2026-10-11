-- =====================================================================
-- V8 · sales-service · Cantidad de boletos de cada venta
-- Hace falta para la saga: cuando llega CarteraDebitada, la venta en línea se confirma solo si todavía
-- tiene reservados TODOS sus asientos. Si la reserva venció y otra venta se llevó alguno, se rechaza
-- y se devuelve el dinero (no se emiten menos boletos de los que se cobraron).
-- También la usan BoletoVendido y VentaCancelada (cantidadBoletos) y el precio por boleto
-- (monto_total / cantidad_boletos), que no cambia aunque Trips cambie el precio del viaje después.
-- Las ventas que ya existían toman la cantidad de sus boletos o de sus asientos.
-- =====================================================================

ALTER TABLE ventas ADD COLUMN cantidad_boletos INT;

UPDATE ventas v
   SET cantidad_boletos = (SELECT count(*) FROM boletos b WHERE b.venta_id = v.id)
 WHERE EXISTS (SELECT 1 FROM boletos b WHERE b.venta_id = v.id);

UPDATE ventas v
   SET cantidad_boletos = (SELECT count(*) FROM asientos_viaje a WHERE a.venta_id = v.id)
 WHERE v.cantidad_boletos IS NULL
   AND EXISTS (SELECT 1 FROM asientos_viaje a WHERE a.venta_id = v.id);

UPDATE ventas SET cantidad_boletos = 1 WHERE cantidad_boletos IS NULL;

ALTER TABLE ventas ALTER COLUMN cantidad_boletos SET NOT NULL;

ALTER TABLE ventas ADD CONSTRAINT ck_ventas_cantidad_boletos CHECK (cantidad_boletos > 0);
