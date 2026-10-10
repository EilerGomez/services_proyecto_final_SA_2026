package gt.cunoc.proyecto2.trips.dtos.operaciones;

import gt.cunoc.proyecto2.trips.modelos.operaciones.SalidaViajeEntidad;
import gt.cunoc.proyecto2.trips.utilidades.Fechas;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** "salida" dentro del detalle de un viaje (T9, T14, T15). */
public record SalidaRespuesta(
        OffsetDateTime fechaHoraSalida,
        BigDecimal kmInicial,
        UUID registradoPor) {

    public SalidaRespuesta(SalidaViajeEntidad salida) {
        this(Fechas.enGuatemala(salida.getFechaHoraSalida()),
                salida.getKmInicial(),
                salida.getRegistradoPor());
    }
}