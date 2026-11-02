package gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Datos de una sucursal en los eventos de creación y actualización de Fleet.
 *
 * <p>Las coordenadas permiten pintar la sucursal en el mapa de rutas.
 */
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
