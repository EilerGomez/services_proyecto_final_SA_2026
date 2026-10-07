package gt.cunoc.proyecto2.fleet.dtos.personal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import java.util.UUID;

/** Cuerpo de F9 · POST /fleet/personal/{id}/traslado. */
public record TrasladarPersonalSolicitud(
        @NotNull UUID sucursalDestinoId,
        @NotNull LocalDate fechaTraslado,
        @NotNull @PositiveOrZero Integer version) {
}