package gt.cunoc.proyecto2.identity.dtos.usuario;


import java.util.Set;
import java.util.UUID;


/**
 * Vista resumida de un usuario con su perfil y roles.
 */
public record UserResponse(


        UUID id,


        String correo,


        String estado,


        Set<String> roles,


        String nombres,


        String apellidos,


        String nit,


        String telefono

) {
}
