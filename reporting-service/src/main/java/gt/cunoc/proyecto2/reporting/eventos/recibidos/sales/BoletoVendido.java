package gt.cunoc.proyecto2.reporting.eventos.recibidos.sales;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Payload de BoletoVendido. Campos según el catálogo (comun README §7). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BoletoVendido(
        UUID ventaId,
        UUID viajeId,
        UUID rutaId,
        UUID busId,
        UUID sucursalId,
        UUID clienteUsuarioId,
        String clienteNit,
        String clienteNombre,
        Integer cantidadBoletos,
        BigDecimal montoTotal,
        LocalDate fechaPago,
        String origenNombre,
        String destinoNombre,
        OffsetDateTime fechaSalida) {
}
