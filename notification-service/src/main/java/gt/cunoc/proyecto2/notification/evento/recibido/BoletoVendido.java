package gt.cunoc.proyecto2.notification.evento.recibido;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/*
 * Compra confirmada publicada por Sales (mostrador o en línea), solo con los campos del correo.
 * clienteCorreo es null en una venta de mostrador sin correo.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BoletoVendido(
        UUID ventaId,
        String clienteNit,
        String clienteNombre,
        String clienteCorreo,
        List<Integer> asientos,
        BigDecimal montoTotal,
        String metodoPago,
        LocalDate fechaPago,
        String origenNombre,
        String destinoNombre,
        OffsetDateTime fechaSalida,
        String busPlaca) {
}