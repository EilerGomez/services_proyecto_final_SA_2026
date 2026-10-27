package gt.cunoc.proyecto2.notification.evento.recibido;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.UUID;

/*
 * Viaje reasignado publicado por Trips: a un viaje programado le cambiaron el bus, el piloto o los
 * dos.
 *
 * Notification solo atiende el caso PRIVADO, que trae el nombre y el correo del cliente del
 * alquiler. En un REGULAR esos campos vienen null: a cada pasajero con boleto le escribe el
 * BoletoReasignado que publica Sales, que sí sabe quiénes compraron.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViajeReasignado(
        UUID viajeId,
        String tipo,
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
}
