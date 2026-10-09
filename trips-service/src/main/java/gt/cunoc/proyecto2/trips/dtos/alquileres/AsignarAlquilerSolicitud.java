package gt.cunoc.proyecto2.trips.dtos.alquileres;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Cuerpo de POST /trips/alquileres/{id}/asignacion.
 * La salida del viaje PRIVADO es la fechaSalida del alquiler; aquí se indica la llegada estimada.
 */
public record AsignarAlquilerSolicitud(
        @NotNull UUID busId,
        @NotNull UUID choferId,
        @NotNull OffsetDateTime fechaLlegadaEstimada,
        @NotNull @PositiveOrZero Integer version) {
}