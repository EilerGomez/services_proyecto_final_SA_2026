package gt.cunoc.proyecto2.trips.dtos.operaciones;

import gt.cunoc.proyecto2.trips.modelos.operaciones.SalidaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Cuerpo de T14 · POST /trips/viajes/{id}/salida. La fecha la elige el usuario. */
public record RegistrarSalidaSolicitud(
        @NotNull OffsetDateTime fechaHoraSalida,
        @NotNull @PositiveOrZero @Digits(integer = 10, fraction = 2) BigDecimal kmInicial) {

    public SalidaViajeEntidad crearEntidad(ViajeEntidad viaje, UUID registradoPor) {
        return SalidaViajeEntidad.registrar(viaje, fechaHoraSalida, kmInicial, registradoPor);
    }
}