package gt.cunoc.proyecto2.identity.dtos.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Cambio de estado de un usuario (ACTIVO, INACTIVO o BLOQUEADO).
 */
public record UserStatusRequest(

        @NotBlank
        @Pattern(regexp = "^(ACTIVO|INACTIVO|BLOQUEADO)$", message = "debe ser ACTIVO, INACTIVO o BLOQUEADO")
        String estado

) {
}
