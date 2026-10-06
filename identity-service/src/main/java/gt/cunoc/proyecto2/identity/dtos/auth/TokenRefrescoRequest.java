package gt.cunoc.proyecto2.identity.dtos.auth;


import jakarta.validation.constraints.NotBlank;


/**
 * Solicitud de renovación del token de acceso.
 */
public record TokenRefrescoRequest(
        @NotBlank String tokenRefresco) {
}
