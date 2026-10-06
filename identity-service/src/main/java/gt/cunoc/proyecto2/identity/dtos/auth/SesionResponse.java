package gt.cunoc.proyecto2.identity.dtos.auth;


import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioSesion;

import java.util.UUID;


/**
 * Respuesta del login y de la renovación (acceso + refresco).
 */
public record SesionResponse(
        String tokenAcceso,
        String tipo,
        String expiraEn,
        String tokenRefresco,
        UUID usuarioId,
        UsuarioSesion usuario) {
}
