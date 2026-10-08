package gt.cunoc.proyecto2.fleet.dtos.personal;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

public record DesactivarPersonalSolicitud(
        @NotNull LocalDate fechaDesasignacion,
        @NotNull @PositiveOrZero Integer version) {
}
