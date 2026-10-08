package gt.cunoc.proyecto2.identity.dtos.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Cambio de contraseña con sesión iniciada.
 */
public record CambioContrasenaRequest(

        @NotBlank
        String contrasenaActual,

        @NotBlank
        @Size(min = 8, max = 72)
        String nuevaContrasena

) {
}
