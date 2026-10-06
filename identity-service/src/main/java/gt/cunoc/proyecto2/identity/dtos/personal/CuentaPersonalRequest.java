package gt.cunoc.proyecto2.identity.dtos.personal;


import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;


/**
 * R5: creación de cuenta de personal o chofer desde Fleet.
 *
 * <p>{@code solicitudId} lo genera Fleet antes de llamar; si la llamada se
 * reintenta se devuelve la misma cuenta. {@code choferId} solo para CHOFER.
 */
public record CuentaPersonalRequest(
        @NotNull UUID solicitudId,
        @NotBlank @Pattern(regexp = "^(CHOFER|ADMIN_SUCURSAL|CAJERO)$") String rol,
        @NotNull UUID sucursalId,
        UUID choferId,
        @NotBlank @Email String correo,
        @NotBlank @Size(max = 100) String nombres,
        @NotBlank @Size(max = 100) String apellidos,
        @NotBlank @Size(max = 20) String nit,
        @NotBlank @Pattern(regexp = "^[0-9]{13}$", message = "debe tener 13 dígitos") String dpi,
        @NotBlank @Size(max = 20) String telefono,
        @NotBlank @Size(max = 255) String direccion,
        @Size(max = 500) String fotoUrl) {
}
