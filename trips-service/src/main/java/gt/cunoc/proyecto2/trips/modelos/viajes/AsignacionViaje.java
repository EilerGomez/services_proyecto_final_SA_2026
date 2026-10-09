package gt.cunoc.proyecto2.trips.modelos.viajes;

import gt.cunoc.proyecto2.trips.errores.viajes.ErrorViaje;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/*
 * Bus, chofer y horario ya validados (R1 y choques de horario) para un viaje.
 * capacidadBus y salarioBaseChofer son las copias que guarda el viaje.
 */
public record AsignacionViaje(
        UUID busId,
        int capacidadBus,
        UUID choferId,
        BigDecimal salarioBaseChofer,
        OffsetDateTime fechaSalidaProgramada,
        OffsetDateTime fechaLlegadaEstimada) {

    public AsignacionViaje {
        verificarFechas(fechaSalidaProgramada, fechaLlegadaEstimada);
    }

    /** 400 FECHAS_INVALIDAS si la llegada no es posterior a la salida. Se llama antes de R1. */
    public static void verificarFechas(OffsetDateTime salida, OffsetDateTime llegada) {
        if (!llegada.isAfter(salida)) {
            throw ErrorViaje.FECHAS_INVALIDAS.excepcion();
        }
    }
}