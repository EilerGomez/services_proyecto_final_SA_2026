package gt.cunoc.proyecto2.reporting.eventos.recibidos.maintenance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de taller registrado en Maintenance, con una línea por gasto.
 *
 * <p>Cada línea se convierte en una fila de gasto de tipo {@code MANO_OBRA} o {@code REPUESTO}; el
 * nombre de la categoría se guarda junto al gasto para que el reporte no dependa de otra consulta.
 */
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
