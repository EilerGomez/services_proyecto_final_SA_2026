package gt.cunoc.proyecto2.identity.dtos.auth;

import jakarta.validation.constraints.NotNull;

/**
 * Activa o desactiva el doble factor de la cuenta propia.
 */
public record DobleFactorRequest(

        @NotNull
        Boolean activado

) {
}
