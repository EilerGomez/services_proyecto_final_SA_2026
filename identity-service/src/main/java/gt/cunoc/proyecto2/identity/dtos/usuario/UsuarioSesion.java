package gt.cunoc.proyecto2.identity.dtos.usuario;


import java.util.Set;
import java.util.UUID;


/**
 * Usuario dentro de la respuesta de sesión.
 */
public record UsuarioSesion(
        UUID usuarioId,
        String correo,
        String nombre,
        Set<String> roles,
        UUID sucursalId,
        UUID choferId,
        String fotoUrl) {
}
