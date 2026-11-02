package gt.cunoc.proyecto2.reporting.reportes.filas;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/** Sucursal para el mapa (Leaflet). */
@Value
@Builder
public class SucursalMapaFila {

    UUID sucursalId;
    String nombre;
    BigDecimal latitud;
    BigDecimal longitud;
    Boolean activa;
}
