package gt.cunoc.proyecto2.fleet.dtos.personal;

import com.fasterxml.jackson.annotation.JsonCreator;
import gt.cunoc.proyecto2.fleet.dtos.cuentas.DatosCuentaSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.cuentas.UsuarioExistenteSolicitud;
import gt.cunoc.proyecto2.fleet.errores.personal.ErrorPersonal;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

/**
 * Cuerpo de F7 · POST /fleet/sucursales/{id}/personal.
 * Exactamente uno: cuenta (cuenta nueva, R5) o usuarioExistente (persona ya registrada, R5b).
 */
public record NuevoPersonalSolicitud(
        @NotNull RolPersonal rol,
        @NotNull LocalDate fechaAsignacion,
        @Valid DatosCuentaSolicitud cuenta,
        @Valid UsuarioExistenteSolicitud usuarioExistente) {

    /** El JSON usa el constructor canónico (hay otro de conveniencia). */
    @JsonCreator
    public NuevoPersonalSolicitud {
    }

    public NuevoPersonalSolicitud(RolPersonal rol, LocalDate fechaAsignacion, DatosCuentaSolicitud cuenta) {
        this(rol, fechaAsignacion, cuenta, null);
    }

    /** true: asignar una cuenta existente; false: crear una nueva. 400 CUENTA_REQUERIDA si no viene exactamente una. */
    public boolean conUsuarioExistente() {
        return ErrorPersonal.verificarUnaCuenta(cuenta, usuarioExistente);
    }
}
