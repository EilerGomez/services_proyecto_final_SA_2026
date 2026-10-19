package gt.cunoc.proyecto2.trips.modelos.alquileres;

import gt.cunoc.proyecto2.trips.errores.alquileres.ErrorAlquiler;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Lo que pide el cliente: de dónde a dónde, cuándo y para cuántos. fechaRetorno null = solo ida.
 * Las coordenadas del origen y del destino son opcionales (el cliente las marca en el mapa); si
 * vienen, la latitud y la longitud de cada punto deben venir juntas.
 */
public record RecorridoAlquiler(
        String origen,
        String destino,
        OffsetDateTime fechaSalida,
        OffsetDateTime fechaRetorno,
        int cantidadPasajeros,
        BigDecimal kmEstimados,
        BigDecimal origenLatitud,
        BigDecimal origenLongitud,
        BigDecimal destinoLatitud,
        BigDecimal destinoLongitud) {

    private static final BigDecimal MAX_LATITUD = new BigDecimal("90");
    private static final BigDecimal MAX_LONGITUD = new BigDecimal("180");

    public RecorridoAlquiler {
        if (fechaRetorno != null && fechaRetorno.isBefore(fechaSalida)) {
            throw ErrorAlquiler.RETORNO_ANTES_DE_SALIDA.excepcion();
        }
        validarCoordenadas(origenLatitud, origenLongitud);
        validarCoordenadas(destinoLatitud, destinoLongitud);
    }

    /** Un punto sin coordenadas es válido; con coordenadas exige latitud y longitud dentro de rango. */
    private static void validarCoordenadas(BigDecimal latitud, BigDecimal longitud) {
        if (latitud == null && longitud == null) {
            return;
        }
        if (latitud == null || longitud == null) {
            throw ErrorAlquiler.COORDENADAS_INCOMPLETAS.excepcion();
        }
        if (latitud.abs().compareTo(MAX_LATITUD) > 0 || longitud.abs().compareTo(MAX_LONGITUD) > 0) {
            throw ErrorAlquiler.COORDENADAS_FUERA_DE_RANGO.excepcion();
        }
    }
}
