package gt.cunoc.proyecto2.identity.dtos.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Datos que completa una cuenta creada con Google antes de usar el portal.
 * nit es opcional: vacío o "CF" queda sin NIT (se factura como consumidor final).
 */
public record CompletarPerfilRequest(

        @Size(max = 20)
        String nit,

        @NotBlank
        @Pattern(regexp = "^[0-9]{13}$", message = "debe tener 13 dígitos")
        String dpi,

        @NotBlank
        @Pattern(regexp = "^[0-9]{8,20}$", message = "solo números, mínimo 8")
        String telefono,

        @NotBlank
        @Size(max = 255)
        String direccion

) {
}
