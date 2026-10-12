package gt.cunoc.proyecto2.notification.evento.recibido;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/*
 * Pago de un alquiler privado publicado por Sales (cartera o efectivo).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AlquilerPagado(
        UUID ordenId,
        UUID alquilerId,
        String clienteNombre,
        String clienteCorreo,
        String origen,
        String destino,
        BigDecimal monto,
        String metodoPago,
        LocalDate fechaPago) {
}