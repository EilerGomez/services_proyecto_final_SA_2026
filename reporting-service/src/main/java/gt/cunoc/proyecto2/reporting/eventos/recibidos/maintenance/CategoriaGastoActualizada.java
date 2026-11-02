package gt.cunoc.proyecto2.reporting.eventos.recibidos.maintenance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/**
 * Nombre y estado de una categoría de gasto de taller que cambió en Maintenance.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CategoriaGastoActualizada(
        UUID categoriaId,
        String nombre,
        Boolean activa) {
}
