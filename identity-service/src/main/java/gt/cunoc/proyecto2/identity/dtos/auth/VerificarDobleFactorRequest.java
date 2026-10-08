package gt.cunoc.proyecto2.identity.dtos.auth;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;


/**
 * Verificación del código de doble factor enviado al correo.
 */
public record VerificarDobleFactorRequest(
        @NotBlank @Email String correo,
        @NotBlank @Pattern(regexp = "^[0-9]{6}$", message = "debe tener 6 dígitos") String codigo) {
}
