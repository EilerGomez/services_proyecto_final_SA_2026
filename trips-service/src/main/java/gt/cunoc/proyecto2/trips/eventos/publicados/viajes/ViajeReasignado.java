package gt.cunoc.proyecto2.trips.eventos.publicados.viajes;

import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeRespuesta;
import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.TipoViaje;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Payload de ViajeReasignado: a un viaje ya programado le cambiaron el bus, el piloto o los dos.
 *
 * Se publica aparte de ViajeActualizado porque ese también se dispara al mover las horas, y lo que
 * importa aquí es avisarle a quien ya tiene el viaje comprado o contratado.
 *
 * Consumen:
 * - Sales: busca los boletos confirmados del viaje y publica uno BoletoReasignado por cada pasajero
 *   con correo (un REGULAR puede llevar decenas).
 * - Notification: en un PRIVADO le escribe directamente al cliente del alquiler, cuyo nombre y
 *   correo vienen aquí (en un REGULAR llegan nulos: esos pasajeros los resuelve Sales).
 */
public record ViajeReasignado(
        UUID viajeId,
        TipoViaje tipo,
        String origenNombre,
        String destinoNombre,
        OffsetDateTime fechaSalida,
        boolean cambioBus,
        String busPlacaAnterior,
        String busPlaca,
        boolean cambioChofer,
        String choferAnterior,
        String choferNombre,
        String clienteNombre,
        String clienteCorreo) {

    /**
     * Desde las dos respuestas del viaje, antes y después del cambio. El alquiler va solo en un
     * viaje PRIVADO (null en un REGULAR): de él salen el nombre y el correo a los que se escribe.
     */
    public static ViajeReasignado desde(ViajeRespuesta antes, ViajeRespuesta ahora,
                                        AlquilerPrivadoEntidad alquiler) {
        return new ViajeReasignado(ahora.viajeId(), ahora.tipo(), ahora.origenNombre(), ahora.destinoNombre(),
                ahora.fechaSalidaProgramada(),
                cambio(antes.busId(), ahora.busId()), antes.busPlaca(), ahora.busPlaca(),
                cambio(antes.choferId(), ahora.choferId()), antes.choferNombre(), ahora.choferNombre(),
                alquiler == null ? null : alquiler.getClienteNombre(),
                alquiler == null ? null : alquiler.getClienteCorreo());
    }

    /** ¿Cambió el bus o el piloto? Si no cambió ninguno de los dos, el evento no se publica. */
    public static boolean huboCambio(ViajeRespuesta antes, ViajeRespuesta ahora) {
        return cambio(antes.busId(), ahora.busId()) || cambio(antes.choferId(), ahora.choferId());
    }

    private static boolean cambio(UUID anterior, UUID actual) {
        return !Objects.equals(anterior, actual);
    }
}
