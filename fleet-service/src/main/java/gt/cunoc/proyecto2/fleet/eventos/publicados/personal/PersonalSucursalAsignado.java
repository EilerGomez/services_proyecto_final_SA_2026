package gt.cunoc.proyecto2.fleet.eventos.publicados.personal;

import gt.cunoc.proyecto2.fleet.modelos.personal.PersonalSucursalEntidad;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import java.time.LocalDate;
import java.util.UUID;

/** Payload de PersonalSucursalAsignado. Consume: Identity (pone sucursal y rol al usuario). */
public record PersonalSucursalAsignado(
        UUID personalId,
        UUID usuarioId,
        UUID sucursalId,
        RolPersonal rolCodigo,
        LocalDate fechaAsignacion) {

    public static PersonalSucursalAsignado desde(PersonalSucursalEntidad personal) {
        return new PersonalSucursalAsignado(personal.getId(), personal.getUsuarioId(), personal.getSucursalId(),
                personal.getRol(), personal.getFechaAsignacion());
    }
}