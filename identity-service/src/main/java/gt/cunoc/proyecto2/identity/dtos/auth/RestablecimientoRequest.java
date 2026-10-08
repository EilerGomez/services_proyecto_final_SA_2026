package gt.cunoc.proyecto2.identity.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Restablecimiento de contraseña con token de recuperación.
 */
public record RestablecimientoRequest(

        @NotBlank
        String token,

        @NotBlank
        @Size(min = 8, max = 72)
        String nuevaContrasena

) {
}
