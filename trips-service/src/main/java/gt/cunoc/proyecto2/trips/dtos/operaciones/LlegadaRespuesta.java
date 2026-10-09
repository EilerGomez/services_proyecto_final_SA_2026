package gt.cunoc.proyecto2.trips.dtos.operaciones;

import gt.cunoc.proyecto2.trips.modelos.operaciones.LlegadaViajeEntidad;
import gt.cunoc.proyecto2.trips.utilidades.Fechas;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** "llegada" dentro del detalle de un viaje (T9, T15), con los costos calculados. */
public record LlegadaRespuesta(
        OffsetDateTime fechaHoraLlegada,
        BigDecimal kmFinal,
        BigDecimal kmRecorridos,
        BigDecimal costoCombustible,
        BigDecimal tarifaDepreciacion,
        BigDecimal costoDepreciacion,
        BigDecimal salarioChofer) {

    public LlegadaRespuesta(LlegadaViajeEntidad llegada) {
        this(Fechas.enGuatemala(llegada.getFechaHoraLlegada()),
                llegada.getKmFinal(),
                llegada.getKmRecorridos(),
                llegada.getCostoCombustible(),
                llegada.getTarifaDepreciacion(),
                llegada.getCostoDepreciacion(),
                llegada.getSalarioChofer());
    }
}