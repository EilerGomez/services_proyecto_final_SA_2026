package gt.cunoc.proyecto2.identity.dtos.auth;


import gt.cunoc.proyecto2.identity.dtos.usuario.UserResponse;

import java.util.UUID;


/**
 * Respuesta del inicio de sesión y de la renovación (access + refresh).
 */
public record SesionResponse(
        String token,
        String tipo,
        long expiraEnSegundos,
        String tokenRefresco,
        UUID usuarioId,
        UserResponse usuario) {
}
