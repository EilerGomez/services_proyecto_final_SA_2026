package gt.cunoc.proyecto2.reporting.reportes.filas;

import java.math.BigDecimal;
import java.util.UUID;
import lombok.Builder;
import lombok.Value;

/** Ruta para el mapa (Leaflet) y para el PDF. */
@Value
@Builder
public class RutaMapaFila {

    UUID rutaId;
    UUID sucursalOrigenId;
    UUID sucursalDestinoId;
    String origen;
    String destino;
    BigDecimal distanciaKm;
    BigDecimal precioBoleto;
    Boolean activa;
}
