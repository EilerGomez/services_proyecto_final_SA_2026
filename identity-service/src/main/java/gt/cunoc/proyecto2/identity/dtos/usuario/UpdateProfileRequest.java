package gt.cunoc.proyecto2.identity.dtos.usuario;


import jakarta.validation.constraints.Size;


/**
 * Edición del perfil propio (todos los campos opcionales).
 */
public record UpdateProfileRequest(


        @Size(max = 100)
        String nombres,


        @Size(max = 100)
        String apellidos,


        @Size(max = 20)
        String telefono,


        @Size(max = 255)
        String direccion,


        @Size(max = 500)
        String fotoUrl

) {
}
