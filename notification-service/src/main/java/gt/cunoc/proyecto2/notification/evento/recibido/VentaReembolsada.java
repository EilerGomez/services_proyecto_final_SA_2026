package gt.cunoc.proyecto2.notification.evento.recibido;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/*
 * Venta reembolsada publicada por Sales: la sucursal devolvió en efectivo el dinero de una compra
 * cancelada (por ejemplo, porque se eliminó el viaje).
 * origenNombre, destinoNombre y fechaSalida pueden venir null si el viaje ya no existe; en ese caso
 * el viaje se reconoce por el motivo, que lo lleva escrito.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VentaReembolsada(
        UUID ventaId,
        String codigo,
        String clienteNombre,
        String clienteCorreo,
        List<Integer> asientos,
        BigDecimal montoReembolsado,
        String metodoPago,
        String motivo,
        LocalDate fechaReembolso,
        String origenNombre,
        String destinoNombre,
        OffsetDateTime fechaSalida) {
}
