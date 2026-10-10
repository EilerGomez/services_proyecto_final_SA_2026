package gt.cunoc.proyecto2.reporting.eventos.recibidos.maintenance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Payload de GastoTallerRegistrado: una fila de gasto por línea (MANO_OBRA o REPUESTO). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record GastoTallerRegistrado(
        UUID servicioId,
        UUID busId,
        UUID sucursalId,
        LocalDate fechaServicio,
        List<Linea> lineas) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Linea(
            UUID lineaId,
            UUID categoriaId,
            String categoriaNombre,
            String descripcion,
            String tipo,
            BigDecimal monto) {
    }
}
