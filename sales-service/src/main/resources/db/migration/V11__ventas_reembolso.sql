-- =====================================================================
-- V11 · sales-service · Reembolso de ventas presenciales
-- Una venta en línea se paga con la cartera, así que al cancelarla el dinero vuelve solo
-- (ReembolsoCarteraSolicitado → CarteraReembolsada → REEMBOLSADA). La venta presencial se pagó en
-- efectivo: alguien tiene que entregar el dinero en la sucursal, y hasta entonces la venta se queda
-- CANCELADA. Esta migración deja registro de esa devolución.
--
-- motivo_cancelacion crece a 500: cuando se elimina un viaje, el motivo lleva también el origen, el
--   destino, la salida y la placa, porque la fila del viaje se borra de vista_viajes y después ya no
--   hay de dónde sacarlos (es lo único que identifica el viaje en la pantalla de reembolsos).
-- fecha_reembolso y reembolsado_por: cuándo se devolvió el dinero y quién lo entregó (null cuando lo
--   devolvió la cartera, porque ahí no interviene ningún cajero).
-- Índice por código: el boleto impreso lleva como código de barras los primeros 12 caracteres del id
--   de la venta, en mayúsculas. El índice es funcional para no guardar una columna que se puede
--   calcular, y no es único a propósito: con 48 bits la repetición es improbable pero posible, y es
--   mejor mostrar las dos ventas que rechazar una venta nueva.
-- =====================================================================

ALTER TABLE ventas ALTER COLUMN motivo_cancelacion TYPE VARCHAR(500);

ALTER TABLE ventas ADD COLUMN fecha_reembolso DATE;
ALTER TABLE ventas ADD COLUMN reembolsado_por UUID;

-- Las ventas que ya estaban reembolsadas (reembolso de la cartera) toman la fecha de la cancelación
UPDATE ventas
   SET fecha_reembolso = coalesce(fecha_cancelacion, fecha_pago)
 WHERE estado = 'REEMBOLSADA'
   AND fecha_reembolso IS NULL;

ALTER TABLE ventas ADD CONSTRAINT ck_ventas_reembolso CHECK (
  estado <> 'REEMBOLSADA' OR fecha_reembolso IS NOT NULL);

CREATE INDEX ix_ventas_codigo ON ventas (upper(substr(replace(id::text, '-', ''), 1, 12)));

-- Pantalla de reembolsos: ventas canceladas o ya reembolsadas de una sucursal
CREATE INDEX ix_ventas_por_devolver ON ventas (sucursal_id, fecha_cancelacion)
  WHERE estado IN ('CANCELADA', 'REEMBOLSADA');
