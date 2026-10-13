package gt.cunoc.proyecto2.maintenance.dtos.servicios;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record LineaServicioSolicitud(
        @NotNull UUID categoriaId,
        @NotBlank @Size(max = 255) String descripcion,
        @NotNull @Pattern(regexp = "MANO_OBRA|REPUESTO") String tipo,
        @NotNull @DecimalMin(value = "0.01") @Digits(integer = 10, fraction = 2) BigDecimal monto) {

    public LineaServicioSolicitud {
        descripcion = descripcion == null ? null : descripcion.strip();
    }
}
