package gt.cunoc.proyecto2.reporting.eventos.recibidos.maintenance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Payload de CategoriaGastoActualizada. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record CategoriaGastoActualizada(
        UUID categoriaId,
        String nombre,
        Boolean activa) {
}
