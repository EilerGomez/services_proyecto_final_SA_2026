package gt.cunoc.proyecto2.trips.dtos.alquileres;

import gt.cunoc.proyecto2.trips.modelos.alquileres.ConfiguracionAlquilerEntidad;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Cuerpo de T2 · POST /trips/configuracion-alquiler.
 * vigenteDesde: fecha elegida por el usuario desde la que aplica el precio (puede ser futura).
 */
public record NuevaConfiguracionAlquilerSolicitud(
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal precioPorKm,
        @NotNull LocalDate vigenteDesde) {

    /** creadoPor: el usuarioId del token. */
    public ConfiguracionAlquilerEntidad crearEntidad(UUID creadoPor) {
        return ConfiguracionAlquilerEntidad.registrar(precioPorKm, vigenteDesde, creadoPor);
    }
}