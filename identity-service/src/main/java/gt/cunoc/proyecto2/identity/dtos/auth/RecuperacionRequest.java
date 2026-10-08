package gt.cunoc.proyecto2.identity.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Solicitud de recuperación de contraseña.
 */
public record RecuperacionRequest(

        @NotBlank
        @Email
        String correo

) {
}
