package gt.cunoc.proyecto2.sales.modelos.alquileres;

import java.math.BigDecimal;

/**
 * Puntos del mapa del alquiler, como llegan en la cotización. Los cuatro pueden venir nulos: las
 * solicitudes viejas no marcaron el recorrido. Latitud y longitud de un punto van juntas o no van,
 * así que un punto a medias se descarta entero (Trips ya lo valida, esto solo evita guardar basura).
 */
public record RecorridoOrden(BigDecimal origenLatitud, BigDecimal origenLongitud,
                             BigDecimal destinoLatitud, BigDecimal destinoLongitud) {

    public static final RecorridoOrden SIN_MAPA = new RecorridoOrden(null, null, null, null);

    public RecorridoOrden {
        if (origenLatitud == null || origenLongitud == null) {
            origenLatitud = null;
            origenLongitud = null;
        }
        if (destinoLatitud == null || destinoLongitud == null) {
            destinoLatitud = null;
            destinoLongitud = null;
        }
    }
}
