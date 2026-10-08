package gt.cunoc.proyecto2.fleet.dtos.personal;

import gt.cunoc.proyecto2.fleet.modelos.personal.PersonalSucursalEntidad;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import java.time.LocalDate;
import java.util.UUID;

/** Respuesta de F6–F9 (y del admin inicial en F3). */
public record PersonalSucursalRespuesta(
        UUID personalId,
        UUID sucursalId,
        UUID usuarioId,
        String nombreCompleto,
        String correo,
        RolPersonal rol,
        boolean activo,
        LocalDate fechaAsignacion,
        LocalDate fechaDesasignacion,
        Integer version) {

    public PersonalSucursalRespuesta(PersonalSucursalEntidad personal) {
        this(personal.getId(),
                personal.getSucursalId(),
                personal.getUsuarioId(),
                personal.getNombreCompleto(),
                personal.getCorreo(),
                personal.getRol(),
                personal.isActivo(),
                personal.getFechaAsignacion(),
                personal.getFechaDesasignacion(),
                personal.getVersion());
    }
}