package gt.cunoc.proyecto2.trips.eventos.publicados.alquileres;

import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.alquileres.EstadoAlquiler;
import gt.cunoc.proyecto2.trips.utilidades.Fechas;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Payload de AlquilerCancelado. Trips ya canceló el alquiler y borró su viaje; el evento le dice a
 * Sales que decida qué pasa con el dinero (y más adelante a Notification que avise al cliente).
 *
 * <p>No lleva el porcentaje de reembolso a propósito: lleva los datos crudos —en qué estado estaba el
 * alquiler y cuántas horas faltaban para la salida— y Sales resuelve la tarifa con tarifas_reembolso,
 * que es la tabla dueña de la política. Así el número que se paga siempre sale de ahí y queda guardado
 * en la orden para poder explicarlo después.</p>
 *
 * <p>Es un evento distinto de ViajeEliminado a propósito: un viaje se borra por muchas razones (un
 * viaje regular que no se vendió, uno mal creado) y un evento que mueve dinero necesita una causa
 * única. ViajeEliminado se sigue publicando, pero solo para las proyecciones.</p>
 */
public record AlquilerCancelado(
        UUID alquilerId,
        UUID sucursalId,
        /** Estado en el que estaba el alquiler justo antes de cancelarse: fija el tramo de la tarifa. */
        EstadoAlquiler estadoAnterior,
        OffsetDateTime fechaSalida,
        OffsetDateTime fechaHoraCancelacion,
        /** Horas completas que faltaban para la salida; negativa si el viaje ya debió salir. */
        long horasAntesDeSalida,
        String motivo,
        /** Viaje PRIVADO que se borró al cancelar, o null si el alquiler no tenía bus asignado. */
        UUID viajeId,
        String clienteNombre,
        String clienteCorreo) {

    public static AlquilerCancelado desde(AlquilerPrivadoEntidad alquiler, EstadoAlquiler estadoAnterior,
                                          OffsetDateTime cancelacion, long horasAntesDeSalida, UUID viajeId) {
        return new AlquilerCancelado(
                alquiler.getId(),
                alquiler.getSucursalId(),
                estadoAnterior,
                Fechas.enGuatemala(alquiler.getFechaSalida()),
                Fechas.enGuatemala(cancelacion),
                horasAntesDeSalida,
                alquiler.getMotivoCancelacion(),
                viajeId,
                alquiler.getClienteNombre(),
                alquiler.getClienteCorreo());
    }
}
