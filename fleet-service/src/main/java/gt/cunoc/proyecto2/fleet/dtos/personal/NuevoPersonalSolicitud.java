package gt.cunoc.proyecto2.fleet.dtos.personal;

import gt.cunoc.proyecto2.fleet.dtos.cuentas.DatosCuentaSolicitud;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/** Cuerpo de F7 · POST /fleet/sucursales/{id}/personal. */
public record NuevoPersonalSolicitud(
        @NotNull RolPersonal rol,
        @NotNull LocalDate fechaAsignacion,
        @NotNull @Valid DatosCuentaSolicitud cuenta) {
}