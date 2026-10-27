package gt.cunoc.proyecto2.notification.evento.recibido;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/*
 * Boleto reasignado publicado por Sales: al viaje de este boleto le cambiaron el bus, el piloto o
 * los dos. Sales publica uno por cada pasajero con venta confirmada y correo.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BoletoReasignado(
        UUID ventaId,
        String codigo,
        UUID viajeId,
        String clienteNombre,
        String clienteCorreo,
        List<Integer> asientos,
        String origenNombre,
        String destinoNombre,
        OffsetDateTime fechaSalida,
        boolean cambioBus,
        String busPlacaAnterior,
        String busPlaca,
        boolean cambioChofer,
        String choferAnterior,
        String choferNombre) {
}
