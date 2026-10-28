-- =====================================================================
-- V14 · sales-service · Cancelación y reembolso de una orden de alquiler
-- Hasta ahora una orden de alquiler solo se reembolsaba por un cobro de más (débito tardío) y nunca
-- se cancelaba. Desde el momento en que el alquiler se puede cancelar aunque ya esté pagado, la orden
-- necesita un estado CANCELADA y el registro de cuánto se devolvió y por qué.
--
-- Estados (nuevo tramo): PAGADA → CANCELADA → REEMBOLSADA.
--   CANCELADA  = el alquiler se canceló y hay dinero pendiente de devolver.
--   REEMBOLSADA = el dinero ya volvió: lo acreditó Wallet (CarteraReembolsada) o lo entregó la
--                 ventanilla en efectivo, igual que una venta (S14).
--   Una orden que se cancela sin haberse pagado pasa directo a CANCELADA con monto_reembolsado = 0
--   y su fecha de reembolso, para que no aparezca en la pantalla de reembolsos.
--
-- porcentaje_reembolso / justificacion_reembolso / horas_antes_viaje: la regla de tarifas_reembolso
--   que se aplicó, copiada en la orden. Se guarda el resultado y no solo la referencia porque la
--   tabla de tarifas cambia con el tiempo y el reporte tiene que poder explicar un reembolso viejo.
-- =====================================================================

-- El estado CANCELADA y la relajación de ck_ordenes_pago: las dos restricciones mencionan
-- 'PENDIENTE_PAGO', así que se borran por su definición (la del estado nació sin nombre propio).
DO $$
DECLARE restriccion TEXT;
BEGIN
  FOR restriccion IN
    SELECT con.conname
      FROM pg_constraint con
      JOIN pg_class rel ON rel.oid = con.conrelid
     WHERE rel.relname = 'ordenes_alquiler'
       AND con.contype = 'c'
       AND pg_get_constraintdef(con.oid) LIKE '%PENDIENTE_PAGO%'
  LOOP
    EXECUTE format('ALTER TABLE ordenes_alquiler DROP CONSTRAINT %I', restriccion);
  END LOOP;
END $$;

ALTER TABLE ordenes_alquiler
  ADD COLUMN fecha_cancelacion       DATE,
  ADD COLUMN motivo_cancelacion      VARCHAR(500),
  ADD COLUMN horas_antes_viaje       INT,
  ADD COLUMN porcentaje_reembolso    NUMERIC(5,2),
  ADD COLUMN justificacion_reembolso VARCHAR(255),
  ADD COLUMN monto_reembolsado       NUMERIC(12,2),
  ADD COLUMN fecha_reembolso         DATE,
  ADD COLUMN reembolsado_por         UUID;

ALTER TABLE ordenes_alquiler ADD CONSTRAINT ck_ordenes_estado CHECK (
  estado IN ('PENDIENTE_PAGO', 'PAGADA', 'RECHAZADA', 'REEMBOLSADA', 'CANCELADA'));

-- Pagada o rechazada lleva método y fecha de pago; cancelada sin pagar no tiene ninguno de los dos
ALTER TABLE ordenes_alquiler ADD CONSTRAINT ck_ordenes_pago CHECK (
  estado IN ('PENDIENTE_PAGO', 'CANCELADA') OR (metodo_pago IS NOT NULL AND fecha_pago IS NOT NULL));

ALTER TABLE ordenes_alquiler ADD CONSTRAINT ck_ordenes_cancelacion CHECK (
  estado NOT IN ('CANCELADA', 'REEMBOLSADA')
  OR fecha_cancelacion IS NULL
  OR motivo_cancelacion IS NOT NULL);

-- El porcentaje aplicado solo tiene sentido entre 0 y 100
ALTER TABLE ordenes_alquiler ADD CONSTRAINT ck_ordenes_porcentaje CHECK (
  porcentaje_reembolso IS NULL OR (porcentaje_reembolso >= 0 AND porcentaje_reembolso <= 100));

-- Lo devuelto nunca puede pasar de lo cobrado
ALTER TABLE ordenes_alquiler ADD CONSTRAINT ck_ordenes_monto_reembolsado CHECK (
  monto_reembolsado IS NULL OR (monto_reembolsado >= 0 AND monto_reembolsado <= monto));

-- Pantalla de reembolsos: órdenes de la sucursal con dinero pendiente de devolver
CREATE INDEX ix_ordenes_por_devolver ON ordenes_alquiler (sucursal_id, fecha_cancelacion)
  WHERE estado = 'CANCELADA';
