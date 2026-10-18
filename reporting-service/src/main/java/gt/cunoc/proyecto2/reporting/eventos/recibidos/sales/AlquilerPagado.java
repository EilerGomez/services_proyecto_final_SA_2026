package gt.cunoc.proyecto2.reporting.eventos.recibidos.sales;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Payload de AlquilerPagado. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AlquilerPagado(
        UUID ordenId,
        UUID alquilerId,
        UUID sucursalId,
        String clienteNit,
        String clienteNombre,
        String origen,
        String destino,
        OffsetDateTime fechaSalida,
        OffsetDateTime fechaRetorno,
        BigDecimal monto,
        BigDecimal montoTotal,
        LocalDate fechaPago) {
}
