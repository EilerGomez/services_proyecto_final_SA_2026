package gt.cunoc.proyecto2.identity.dtos.auth;

import gt.cunoc.proyecto2.identity.models.Perfil;
import gt.cunoc.proyecto2.identity.models.Usuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.NumberFormat;

/**
 * Registro público de cliente (solo asigna CLIENTE).
 */
public record RegistroRequest(

        @NotBlank
        @Email
        String correo,

        @NotBlank
        @Size(min = 8, max = 72)
        String contrasena,

        @NotBlank
        @Size(max = 100)
        String nombres,

        @NotBlank
        @Size(max = 100)
        String apellidos,

        @NotBlank
        @Size(max = 20)
        String nit,

        @NotBlank
        @Pattern(regexp = "^[0-9]{13}$", message = "debe tener 13 dígitos")
        String dpi,

        @NotBlank
        @Pattern(regexp = "\\d+", message = "El teléfono debe contener solo números")
        String telefono,

        @NotBlank
        @Size(max = 255)
        String direccion,

        @Size(max = 500)
        String fotoUrl

) {
        public Perfil createEntity(Usuario usuario) {
                Perfil perfil = new Perfil();
                perfil.setUsuario(usuario);
                perfil.setNombres(nombres);
                perfil.setApellidos(apellidos);
                perfil.setNit(nit);
                perfil.setDpi(dpi);
                perfil.setTelefono(telefono);
                perfil.setDireccion(direccion);
                perfil.setFotoUrl(fotoUrl);
                return perfil;
        }

}
