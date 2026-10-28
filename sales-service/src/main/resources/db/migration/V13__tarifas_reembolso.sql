-- =====================================================================
-- V13 · sales-service · Tarifas de reembolso por cancelación de alquiler
-- Política de dinero: cuánto se le devuelve al cliente cuando se cancela su alquiler, según en qué
-- estado estaba el alquiler y cuántas horas faltaban para la salida. Vive en Sales porque Sales es
-- quien tiene la orden de alquiler, calcula el monto y ejecuta la devolución (cartera o ventanilla).
--
-- Resolución (una sola regla, determinista): entre las filas activas cuyo estado_alquiler sea el del
--   alquiler —o CUALQUIERA— y cuyo horas_minimas_antes no pase de las horas que faltaban, gana la de
--   horas_minimas_antes más grande; si empatan, la del estado específico antes que la de CUALQUIERA.
--   La fila con horas_minimas_antes = 0 es el caso de último momento y nunca debe faltar.
--   Si ninguna fila aplica (tabla vacía), el servicio permite cancelar y devuelve el 100%: quedarse
--   con el dinero del cliente sin una regla que lo justifique sería peor.
--
-- permite_cancelacion = false: la cancelación se rechaza (422) y no se toca ni el alquiler ni el viaje.
--   En ese caso el porcentaje tiene que ser 0, porque no hay nada que devolver (ck_tarifas_sin_reembolso).
--
-- Trips necesita saber si se permite cancelar ANTES de borrar el viaje, así que lleva una copia de
--   solo lectura (vista_tarifas_reembolso, V16 de trips) que se mantiene con TarifaReembolsoCambiada.
--   El porcentaje que se paga siempre lo calcula Sales con esta tabla, que es la dueña.
--
-- Publica: TarifaReembolsoCambiada (al crear, editar o desactivar una tarifa).
-- =====================================================================

CREATE TABLE tarifas_reembolso (
  id                    UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  estado_alquiler       VARCHAR(12)   NOT NULL
                        CHECK (estado_alquiler IN ('SOLICITADO', 'COTIZADO', 'PAGADO', 'ASIGNADO', 'CUALQUIERA')),
  horas_minimas_antes   INT           NOT NULL CHECK (horas_minimas_antes >= 0),
  permite_cancelacion   BOOLEAN       NOT NULL DEFAULT TRUE,
  porcentaje_reembolso  NUMERIC(5,2)  NOT NULL CHECK (porcentaje_reembolso >= 0 AND porcentaje_reembolso <= 100),
  justificacion         VARCHAR(255)  NOT NULL,
  activa                BOOLEAN       NOT NULL DEFAULT TRUE,
  version               INT           NOT NULL DEFAULT 0,
  fecha_creacion        TIMESTAMPTZ   NOT NULL DEFAULT now(),
  fecha_actualizacion   TIMESTAMPTZ   NOT NULL DEFAULT now(),
  -- Dos filas no pueden competir por el mismo tramo del mismo estado
  CONSTRAINT uq_tarifas_reembolso_tramo UNIQUE (estado_alquiler, horas_minimas_antes),
  CONSTRAINT ck_tarifas_sin_reembolso CHECK (permite_cancelacion OR porcentaje_reembolso = 0)
);

-- La resolución filtra por estado y ordena por el tramo
CREATE INDEX ix_tarifas_reembolso ON tarifas_reembolso (estado_alquiler, horas_minimas_antes DESC)
  WHERE activa;

CREATE TRIGGER tr_tarifas_reembolso_fecha BEFORE UPDATE ON tarifas_reembolso
  FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();

-- ---------------------------------------------------------------------
-- Política inicial. Los ids son fijos a propósito: la misma fila se siembra en la copia de Trips
-- (V16 de trips) con el mismo id, porque una migración no publica eventos y sin esto Trips arrancaría
-- sin política. Si el administrador del sistema edita una tarifa, el UPSERT por id actualiza la copia.
--
-- Son EDITABLES desde la pantalla de tarifas de reembolso; acá solo están para que el sistema no
-- arranque sin política.
--   SOLICITADO / COTIZADO : se cancela en cualquier momento. No se había pagado, así que no hay nada
--                           que devolver (el porcentaje queda en 0 porque no hay monto al que aplicarlo).
--   PAGADO                : se cancela en cualquier momento y se devuelve el 95%.
--   ASIGNADO              : ya hay bus y piloto reservados, así que la penalización depende del tiempo:
--                           24 h o más → 80%; menos de 24 h → 60%; a la hora de salida ya no se cancela.
--                           El último tramo es horas_minimas_antes = 0, es decir "falta menos de una
--                           hora o el viaje ya salió": si quiere un margen mayor (dos horas, por
--                           ejemplo), suba el horas_minimas_antes de la fila del 60%.
-- ---------------------------------------------------------------------
INSERT INTO tarifas_reembolso
  (id, estado_alquiler, horas_minimas_antes, permite_cancelacion, porcentaje_reembolso, justificacion)
VALUES
  ('7a4f0001-0000-4000-8000-000000000001', 'SOLICITADO', 0, TRUE, 0.00,
   'El alquiler todavía no se ha cotizado ni pagado: se cancela en cualquier momento y no hay nada que devolver'),
  ('7a4f0001-0000-4000-8000-000000000002', 'COTIZADO', 0, TRUE, 0.00,
   'El alquiler ya tiene precio pero aún no se ha pagado: se cancela en cualquier momento y no hay nada que devolver'),
  ('7a4f0001-0000-4000-8000-000000000003', 'PAGADO', 0, TRUE, 95.00,
   'Alquiler pagado al que todavía no se le asigna bus ni piloto: se devuelve el 95% y se retiene el 5% por gastos administrativos'),
  ('7a4f0001-0000-4000-8000-000000000004', 'ASIGNADO', 24, TRUE, 80.00,
   'Cancelación con 24 horas o más de anticipación: se devuelve el 80% porque el bus y el piloto ya estaban reservados y se pueden reprogramar'),
  ('7a4f0001-0000-4000-8000-000000000005', 'ASIGNADO', 1, TRUE, 60.00,
   'Cancelación con menos de 24 horas para la salida: se devuelve el 60% porque a esa hora el bus y el piloto ya no se pueden reasignar'),
  ('7a4f0001-0000-4000-8000-000000000006', 'ASIGNADO', 0, FALSE, 0.00,
   'A la hora de la salida el alquiler ya no se puede cancelar: la unidad se presentó y el viaje se registra como realizado');
