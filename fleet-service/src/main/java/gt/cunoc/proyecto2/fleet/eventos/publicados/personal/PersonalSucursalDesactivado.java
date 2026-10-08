package gt.cunoc.proyecto2.fleet.eventos.publicados.personal;

import gt.cunoc.proyecto2.fleet.modelos.personal.PersonalSucursalEntidad;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import java.time.LocalDate;
import java.util.UUID;

/** Payload de PersonalSucursalDesactivado. Consume: Identity (le quita el rol y la sucursal). */
public record PersonalSucursalDesactivado(
        UUID personalId,
        UUID usuarioId,
        UUID sucursalId,
        RolPersonal rolCodigo,
        LocalDate fechaDesasignacion) {

    public static PersonalSucursalDesactivado desde(PersonalSucursalEntidad personal) {
        return new PersonalSucursalDesactivado(personal.getId(), personal.getUsuarioId(), personal.getSucursalId(),
                personal.getRol(), personal.getFechaDesasignacion());
    }
}