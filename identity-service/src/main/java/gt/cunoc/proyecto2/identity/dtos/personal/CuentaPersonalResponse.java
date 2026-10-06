package gt.cunoc.proyecto2.identity.dtos.personal;


import java.util.UUID;


/**
 * Respuesta de R5: 201 cuenta nueva, 200 solicitud ya procesada.
 */
public record CuentaPersonalResponse(
        UUID usuarioId,
        String correo,
        String nombreCompleto,
        boolean creada) {
}
