package gt.cunoc.proyecto2.identity.dtos.usuario;


import jakarta.validation.constraints.NotEmpty;

import java.util.Set;


/**
 * Asignación de roles a un usuario.
 */
public record RolesRequest(


        @NotEmpty
        Set<String> roles

) {
}
