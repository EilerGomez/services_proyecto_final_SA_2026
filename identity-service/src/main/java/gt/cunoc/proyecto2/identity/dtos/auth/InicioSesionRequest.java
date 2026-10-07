package gt.cunoc.proyecto2.identity.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales del inicio de sesión.
 */
public record InicioSesionRequest(

        @NotBlank
        @Email
        String correo,

        @NotBlank
        String contrasena

) {
}
