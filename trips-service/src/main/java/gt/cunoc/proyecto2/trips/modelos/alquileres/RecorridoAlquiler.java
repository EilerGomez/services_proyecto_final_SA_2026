package gt.cunoc.proyecto2.trips.modelos.alquileres;

import gt.cunoc.proyecto2.trips.errores.alquileres.ErrorAlquiler;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** Lo que pide el cliente: de dónde a dónde, cuándo y para cuántos. fechaRetorno null = solo ida. */
public record RecorridoAlquiler(
        String origen,
        String destino,
        OffsetDateTime fechaSalida,
        OffsetDateTime fechaRetorno,
        int cantidadPasajeros,
        BigDecimal kmEstimados) {

    public RecorridoAlquiler {
        if (fechaRetorno != null && fechaRetorno.isBefore(fechaSalida)) {
            throw ErrorAlquiler.RETORNO_ANTES_DE_SALIDA.excepcion();
        }
    }
}