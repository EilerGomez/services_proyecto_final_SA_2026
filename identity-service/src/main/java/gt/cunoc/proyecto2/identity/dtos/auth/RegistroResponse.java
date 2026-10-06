package gt.cunoc.proyecto2.identity.dtos.auth;


import java.util.Set;
import java.util.UUID;


/**
 * Respuesta del registro de cliente.
 */
public record RegistroResponse(
        UUID usuarioId,
        String correo,
        Set<String> roles) {
}
