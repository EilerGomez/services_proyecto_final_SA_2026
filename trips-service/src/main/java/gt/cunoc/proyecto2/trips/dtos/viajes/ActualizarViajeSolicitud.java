package gt.cunoc.proyecto2.trips.dtos.viajes;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.OffsetDateTime;
import java.util.UUID;

/*
 * Cuerpo de · PUT /trips/viajes/{id}: hora, bus o chofer de un viaje PROGRAMADO.
 * Se envían los cuatro datos aunque solo cambie uno. version: bloqueo optimista.
 */
public record ActualizarViajeSolicitud(
        @NotNull UUID busId,
        @NotNull UUID choferId,
        @NotNull OffsetDateTime fechaSalidaProgramada,
        @NotNull OffsetDateTime fechaLlegadaEstimada,
        @NotNull @PositiveOrZero Integer version) {
}