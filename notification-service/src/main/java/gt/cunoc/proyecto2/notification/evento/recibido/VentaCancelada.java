package gt.cunoc.proyecto2.notification.evento.recibido;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/*
 * Venta cancelada publicada por Sales (por un administrador o porque se eliminó el viaje).
 * origenNombre, destinoNombre y fechaSalida pueden venir null si el viaje ya no existe.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VentaCancelada(
        UUID ventaId,
        String clienteNombre,
        String clienteCorreo,
        List<Integer> asientos,
        BigDecimal montoAnulado,
        String metodoPago,
        String motivo,
        LocalDate fechaCancelacion,
        String origenNombre,
        String destinoNombre,
        OffsetDateTime fechaSalida) {
}