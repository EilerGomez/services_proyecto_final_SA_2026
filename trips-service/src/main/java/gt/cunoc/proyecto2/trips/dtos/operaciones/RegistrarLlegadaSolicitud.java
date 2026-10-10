package gt.cunoc.proyecto2.trips.dtos.operaciones;

import gt.cunoc.proyecto2.trips.modelos.operaciones.LlegadaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.operaciones.SalidaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Cuerpo de T15 · POST /trips/viajes/{id}/llegada. La fecha la elige el usuario. */
public record RegistrarLlegadaSolicitud(
        @NotNull OffsetDateTime fechaHoraLlegada,
        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal kmFinal,
        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal costoCombustible) {

    /** tarifaDepreciacion: la vigente el día de llegada (vista_tarifas_depreciacion). */
    public LlegadaViajeEntidad crearEntidad(ViajeEntidad viaje, SalidaViajeEntidad salida,
                                            BigDecimal tarifaDepreciacion, UUID registradoPor) {
        return LlegadaViajeEntidad.registrar(viaje, salida, fechaHoraLlegada, kmFinal, costoCombustible,
                tarifaDepreciacion, registradoPor);
    }
}