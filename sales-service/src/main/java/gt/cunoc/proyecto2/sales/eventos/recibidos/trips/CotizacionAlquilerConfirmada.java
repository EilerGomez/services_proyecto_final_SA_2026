package gt.cunoc.proyecto2.sales.eventos.recibidos.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

/*
 * Payload de CotizacionAlquilerConfirmada (de Trips): crea la orden de pago por precioConfirmado.
 * clienteUsuarioId es null si el alquiler se pidió en mostrador (solo se podrá pagar en efectivo).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CotizacionAlquilerConfirmada(
        UUID alquilerId,
        UUID sucursalId,
        UUID clienteUsuarioId,
        String clienteNit,
        String clienteNombre,
        String clienteCorreo,
        String origen,
        String destino,
        BigDecimal precioConfirmado) {
}
