-- =====================================================================
-- V7 · trips-service · Viajes
-- REGULAR: tiene ruta y precio de boleto (se vende por asiento en Sales).
-- PRIVADO: viene de un alquiler (la FK a alquileres_privados se agrega en V11).
-- capacidad_bus, precio_boleto y salario_base_chofer son copias del momento de programar
-- (los cambios posteriores en Fleet o en la ruta no afectan viajes ya programados).
-- Publica ViajeProgramado / ViajeActualizado / ViajeEliminado / ViajeIniciado / ViajeCompletado.
-- =====================================================================

CREATE TABLE viajes (
  id                        UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
  tipo                      VARCHAR(10)   NOT NULL CHECK (tipo IN ('REGULAR', 'PRIVADO')),
  ruta_id                   UUID          REFERENCES rutas(id) ON DELETE RESTRICT,
  alquiler_id               UUID          UNIQUE,              -- FK en V11 (alquileres_privados)
  sucursal_id               UUID          NOT NULL,            -- sucursal que opera el viaje
  bus_id                    UUID          NOT NULL,
  chofer_id                 UUID          NOT NULL,
  capacidad_bus             INT           NOT NULL CHECK (capacidad_bus > 0),
  precio_boleto             NUMERIC(12,2),                     -- solo REGULAR
  salario_base_chofer       NUMERIC(12,2) NOT NULL CHECK (salario_base_chofer > 0),
  fecha_salida_programada   TIMESTAMPTZ   NOT NULL,            -- fechas elegidas por el usuario
  fecha_llegada_estimada    TIMESTAMPTZ   NOT NULL,
  estado                    VARCHAR(12)   NOT NULL DEFAULT 'PROGRAMADO'
                            CHECK (estado IN ('PROGRAMADO', 'EN_RUTA', 'COMPLETADO')),
  version                   INT           NOT NULL DEFAULT 0,
  creado_por                UUID          NOT NULL,
  fecha_creacion            TIMESTAMPTZ   NOT NULL DEFAULT now(),
  fecha_actualizacion       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  CHECK (fecha_llegada_estimada > fecha_salida_programada),
  CHECK ( (tipo = 'REGULAR' AND ruta_id IS NOT NULL AND alquiler_id IS NULL AND precio_boleto IS NOT NULL)
       OR (tipo = 'PRIVADO' AND alquiler_id IS NOT NULL AND ruta_id IS NULL AND precio_boleto IS NULL) )
);

-- Choques de horario y R3 (viajes activos de un bus o de un chofer)
CREATE INDEX ix_viajes_bus    ON viajes (bus_id, estado);
CREATE INDEX ix_viajes_chofer ON viajes (chofer_id, estado);
-- T8 (búsqueda por ruta y fecha) y T7 (¿la ruta tiene viajes?)
CREATE INDEX ix_viajes_ruta   ON viajes (ruta_id, fecha_salida_programada);
CREATE INDEX ix_viajes_sucursal_salida ON viajes (sucursal_id, fecha_salida_programada);

CREATE TRIGGER tr_viajes_fecha
  BEFORE UPDATE ON viajes
  FOR EACH ROW EXECUTE FUNCTION fn_actualizar_fecha();

-- El tipo de viaje no se puede cambiar
CREATE OR REPLACE FUNCTION fn_tipo_viaje_inmutable() RETURNS trigger AS $$
BEGIN
  IF NEW.tipo <> OLD.tipo THEN
    RAISE EXCEPTION 'El tipo de viaje no se puede modificar';
  END IF;
  RETURN NEW;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER tr_tipo_viaje_inmutable
  BEFORE UPDATE OF tipo ON viajes
  FOR EACH ROW EXECUTE FUNCTION fn_tipo_viaje_inmutable();

-- Solo se eliminan (T12) viajes que no han iniciado; los demás quedan como historial
CREATE OR REPLACE FUNCTION fn_eliminar_solo_programado() RETURNS trigger AS $$
BEGIN
  IF OLD.estado <> 'PROGRAMADO' THEN
    RAISE EXCEPTION 'Solo se pueden eliminar viajes que no han iniciado';
  END IF;
  RETURN OLD;
END; $$ LANGUAGE plpgsql;

CREATE TRIGGER tr_eliminar_solo_programado
  BEFORE DELETE ON viajes
  FOR EACH ROW EXECUTE FUNCTION fn_eliminar_solo_programado();
