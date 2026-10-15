package gt.cunoc.proyecto2.fleet.dtos.personal;

import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Cuerpo de F10 · POST /fleet/personal/{id}/reintegro.
 * sucursalId puede ser la misma de antes u otra; rol puede ser el mismo u otro.
 */
public record ReintegrarPersonalSolicitud(
        @NotNull UUID sucursalId,
        @NotNull RolPersonal rol,
        @NotNull LocalDate fechaAsignacion) {
}
