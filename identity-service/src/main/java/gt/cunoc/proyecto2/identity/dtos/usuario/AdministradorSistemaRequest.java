package gt.cunoc.proyecto2.identity.dtos.usuario;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;


/**
 * Creación de otro ADMIN_SISTEMA (sin contraseña: se activa con enlace).
 */
public record AdministradorSistemaRequest(
        @NotBlank @Email String correo,
        @NotBlank @Size(max = 100) String nombres,
        @NotBlank @Size(max = 100) String apellidos,
        @NotBlank @Size(max = 20) String nit,
        @NotBlank @Pattern(regexp = "^[0-9]{13}$", message = "debe tener 13 dígitos") String dpi,
        @NotBlank @Size(max = 20) String telefono,
        @NotBlank @Size(max = 255) String direccion) {
}
