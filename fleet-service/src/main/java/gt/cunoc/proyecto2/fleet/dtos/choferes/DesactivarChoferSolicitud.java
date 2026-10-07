package gt.cunoc.proyecto2.fleet.dtos.choferes;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

/** Cuerpo de F21 · PATCH /fleet/choferes/{id}/desactivar. fecha: fecha de negocio de ChoferDesactivado. */
public record DesactivarChoferSolicitud(
        @NotNull LocalDate fecha,
        @NotNull @PositiveOrZero Integer version) {
}