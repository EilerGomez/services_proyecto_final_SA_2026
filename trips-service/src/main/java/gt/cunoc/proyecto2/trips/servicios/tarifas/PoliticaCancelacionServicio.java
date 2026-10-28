package gt.cunoc.proyecto2.trips.servicios.tarifas;

import gt.cunoc.proyecto2.trips.dtos.alquileres.PoliticaCancelacionRespuesta;
import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import java.time.Duration;
import java.time.OffsetDateTime;

/**
 * Lee la copia local de las tarifas de reembolso (vista_tarifas_reembolso, V16) para una sola
 * decisión: si una cancelación se permite. El monto que se devuelve lo calcula Sales, que es el dueño
 * de la política; acá el porcentaje solo sirve para avisarle al usuario antes de confirmar.
 */
public interface PoliticaCancelacionServicio {

    /**
     * La tarifa que aplicaría si el alquiler se cancelara en ese momento: la fila activa de
     * horas_minimas_antes más grande que no pase de las horas que faltan, prefiriendo el estado
     * específico sobre el comodín. Sin ninguna fila aplicable, permite cancelar.
     */
    PoliticaCancelacionRespuesta para(AlquilerPrivadoEntidad alquiler, OffsetDateTime cancelacion);

    /** Horas completas entre el momento de la cancelación y la salida; negativa si ya debió salir. */
    static long horasAntesDeSalida(OffsetDateTime cancelacion, OffsetDateTime fechaSalida) {
        return Duration.between(cancelacion, fechaSalida).toHours();
    }
}
