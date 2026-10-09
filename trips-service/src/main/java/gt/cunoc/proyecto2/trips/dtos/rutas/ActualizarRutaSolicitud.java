package gt.cunoc.proyecto2.trips.dtos.rutas;

import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record ActualizarRutaSolicitud(
        @NotNull @DecimalMin("0.01") @Digits(integer = 6, fraction = 2) BigDecimal distanciaKm,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal precioBoleto,
        @NotNull Boolean activa,
        @NotNull @PositiveOrZero Integer version) {

    public void aplicarA(RutaEntidad ruta) {
        ruta.actualizar(distanciaKm, precioBoleto, activa);
    }
}