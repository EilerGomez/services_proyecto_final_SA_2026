package gt.cunoc.proyecto2.notification.evento.recibido;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/*
 * Cotización de alquiler confirmada publicada por Trips: el cliente ya puede pagarla.
 * clienteCorreo puede venir null si el alquiler se pidió en mostrador sin correo.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CotizacionAlquilerConfirmada(
        UUID alquilerId,
        String clienteNombre,
        String clienteCorreo,
        String origen,
        String destino,
        OffsetDateTime fechaSalida,
        OffsetDateTime fechaRetorno,
        int cantidadPasajeros,
        BigDecimal precioConfirmado) {
}