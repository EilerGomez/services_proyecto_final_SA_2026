package gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

/** Payload de SucursalCreada y SucursalActualizada (mismo contenido). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record SucursalEvento(
        UUID sucursalId,
        String codigo,
        String nombre,
        String departamento,
        String municipio,
        BigDecimal latitud,
        BigDecimal longitud,
        boolean activa) {
}
