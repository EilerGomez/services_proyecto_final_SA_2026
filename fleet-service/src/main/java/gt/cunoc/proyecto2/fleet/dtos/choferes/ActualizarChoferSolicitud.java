package gt.cunoc.proyecto2.fleet.dtos.choferes;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Cuerpo de F19 · PUT /fleet/choferes/{id}: datos laborales.
 * Nombre, correo y foto se editan en Identity (llegan por el evento PerfilActualizado).
 */
public record ActualizarChoferSolicitud(
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal salarioBaseViaje,
        @NotNull @PositiveOrZero Integer version) {
}