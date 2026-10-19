package gt.cunoc.proyecto2.notification.evento.recibido;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/*
 * Pago de un alquiler privado publicado por Sales (cartera o efectivo).
 * sucursalId se usa para buscar a sus administradores y avisarles del pago.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AlquilerPagado(
        UUID ordenId,
        UUID alquilerId,
        UUID sucursalId,
        String clienteNombre,
        String clienteCorreo,
        String origen,
        String destino,
        BigDecimal monto,
        String metodoPago,
        LocalDate fechaPago) {
}