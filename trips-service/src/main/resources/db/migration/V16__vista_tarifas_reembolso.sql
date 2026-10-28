-- =====================================================================
-- V16 · trips-service · Vista de tarifas de reembolso (copia local de Sales)
-- La política de cancelación vive en Sales (tabla tarifas_reembolso, V13 de sales), porque Sales es
-- quien tiene la orden de alquiler y ejecuta la devolución del dinero. Trips necesita una sola cosa
-- de esa tabla: saber si una cancelación se permite, ANTES de cancelar el alquiler y borrar el viaje.
--
-- Por eso lleva una copia de solo lectura, igual que vista_tarifas_depreciacion (V9) con Identity.
-- La llena el manejador de TarifaReembolsoCambiada (UPSERT: la tabla sí se edita, a diferencia de las
-- tarifas de depreciación). Trips nunca calcula el monto a devolver: eso lo hace Sales con su tabla.
--
-- Resolución (idéntica a la de Sales): entre las filas activas cuyo estado_alquiler sea el del
--   alquiler —o CUALQUIERA— y cuyo horas_minimas_antes no pase de las horas que faltan, gana la de
--   horas_minimas_antes más grande; si empatan, la del estado específico. Sin filas, se permite.
--
-- También se agrega alquileres_privados.fecha_hora_cancelacion: el momento exacto que indicó quien
--   canceló (lo manda el frontend para poder probar la tabla de porcentajes con distintas fechas).
--   La fecha de solicitud ya se pide así en T16, y acá se sigue el mismo criterio del enunciado.
-- =====================================================================

CREATE TABLE vista_tarifas_reembolso (
  id                    UUID          PRIMARY KEY,          -- tarifaId de Sales
  estado_alquiler       VARCHAR(12)   NOT NULL,
  horas_minimas_antes   INT           NOT NULL,
  permite_cancelacion   BOOLEAN       NOT NULL,
  porcentaje_reembolso  NUMERIC(5,2)  NOT NULL,
  justificacion         VARCHAR(255)  NOT NULL,
  activa                BOOLEAN       NOT NULL
);

-- La consulta filtra por estado y ordena por el tramo
CREATE INDEX ix_vista_tarifas_reembolso ON vista_tarifas_reembolso (estado_alquiler, horas_minimas_antes DESC)
  WHERE activa;

-- Política inicial, con los MISMOS ids que la tabla dueña (V13 de sales): una migración no publica
-- eventos, así que sin esta copia Trips arrancaría sin política y permitiría cualquier cancelación.
-- Cuando el administrador del sistema edite una tarifa, TarifaReembolsoCambiada actualiza la fila por id.
INSERT INTO vista_tarifas_reembolso
  (id, estado_alquiler, horas_minimas_antes, permite_cancelacion, porcentaje_reembolso, justificacion, activa)
VALUES
  ('7a4f0001-0000-4000-8000-000000000001', 'SOLICITADO', 0, TRUE, 0.00,
   'El alquiler todavía no se ha cotizado ni pagado: se cancela en cualquier momento y no hay nada que devolver', TRUE),
  ('7a4f0001-0000-4000-8000-000000000002', 'COTIZADO', 0, TRUE, 0.00,
   'El alquiler ya tiene precio pero aún no se ha pagado: se cancela en cualquier momento y no hay nada que devolver', TRUE),
  ('7a4f0001-0000-4000-8000-000000000003', 'PAGADO', 0, TRUE, 95.00,
   'Alquiler pagado al que todavía no se le asigna bus ni piloto: se devuelve el 95% y se retiene el 5% por gastos administrativos', TRUE),
  ('7a4f0001-0000-4000-8000-000000000004', 'ASIGNADO', 24, TRUE, 80.00,
   'Cancelación con 24 horas o más de anticipación: se devuelve el 80% porque el bus y el piloto ya estaban reservados y se pueden reprogramar', TRUE),
  ('7a4f0001-0000-4000-8000-000000000005', 'ASIGNADO', 1, TRUE, 60.00,
   'Cancelación con menos de 24 horas para la salida: se devuelve el 60% porque a esa hora el bus y el piloto ya no se pueden reasignar', TRUE),
  ('7a4f0001-0000-4000-8000-000000000006', 'ASIGNADO', 0, FALSE, 0.00,
   'A la hora de la salida el alquiler ya no se puede cancelar: la unidad se presentó y el viaje se registra como realizado', TRUE)
ON CONFLICT (id) DO NOTHING;

ALTER TABLE alquileres_privados ADD COLUMN fecha_hora_cancelacion TIMESTAMPTZ;

-- Si está cancelado y se sabe cuándo, el motivo sigue siendo obligatorio (ya lo exige V11)
ALTER TABLE alquileres_privados ADD CONSTRAINT ck_alquileres_cancelacion CHECK (
  fecha_hora_cancelacion IS NULL OR estado = 'CANCELADO');
