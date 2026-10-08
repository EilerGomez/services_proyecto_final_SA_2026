package gt.cunoc.proyecto2.fleet.dtos.personal;

/** Respuesta de F9: la asignación que se cerró y la nueva. */
public record TrasladoPersonalRespuesta(
        PersonalSucursalRespuesta anterior,
        PersonalSucursalRespuesta nueva) {
}