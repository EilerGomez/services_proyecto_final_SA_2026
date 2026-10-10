package gt.cunoc.proyecto2.trips.dtos.alquileres;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** Cuerpo de POST /trips/alquileres/{id}/cotizacion: el precio final (igual o ajustado). */
public record CotizarAlquilerSolicitud(
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal precioConfirmado,
        @NotNull @PositiveOrZero Integer version) {
}