package gt.cunoc.proyecto2.trips.eventos.publicados.alquileres;

import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.alquileres.CanalAlquiler;
import gt.cunoc.proyecto2.trips.utilidades.Fechas;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/*
 * Payload de CotizacionAlquilerConfirmada.
 * Consumen: Sales (crea la orden de pago por precioConfirmado) y Notification (correo al cliente).
 * clienteUsuarioId es null si el alquiler es PRESENCIAL.
 */
public record CotizacionAlquilerConfirmada(
        UUID alquilerId,
        UUID sucursalId,
        CanalAlquiler canal,
        UUID clienteUsuarioId,
        String clienteNit,
        String clienteNombre,
        String clienteCorreo,
        String origen,
        String destino,
        // Puntos que el cliente marcó en el mapa: van en el evento para que la ventanilla pueda ver
        // el recorrido al cobrar la orden. Null en las solicitudes que no se marcaron en el mapa.
        BigDecimal origenLatitud,
        BigDecimal origenLongitud,
        BigDecimal destinoLatitud,
        BigDecimal destinoLongitud,
        OffsetDateTime fechaSalida,
        OffsetDateTime fechaRetorno,
        int cantidadPasajeros,
        BigDecimal precioConfirmado) {

    public static CotizacionAlquilerConfirmada desde(AlquilerPrivadoEntidad alquiler) {
        return new CotizacionAlquilerConfirmada(alquiler.getId(), alquiler.getSucursalId(), alquiler.getCanal(),
                alquiler.getClienteUsuarioId(), alquiler.getClienteNit(), alquiler.getClienteNombre(),
                alquiler.getClienteCorreo(), alquiler.getOrigen(), alquiler.getDestino(),
                alquiler.getOrigenLatitud(), alquiler.getOrigenLongitud(),
                alquiler.getDestinoLatitud(), alquiler.getDestinoLongitud(),
                Fechas.enGuatemala(alquiler.getFechaSalida()), Fechas.enGuatemala(alquiler.getFechaRetorno()),
                alquiler.getCantidadPasajeros(), alquiler.getPrecioConfirmado());
    }
}