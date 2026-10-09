package gt.cunoc.proyecto2.trips.dtos.rutas;

import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** Cuerpo de T5 · POST /trips/rutas. El origen debe ser la sucursal del token. */
public record NuevaRutaSolicitud(
        @NotNull UUID sucursalOrigenId,
        @NotNull UUID sucursalDestinoId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 6, fraction = 2) BigDecimal distanciaKm,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal precioBoleto) {

    /** creadoPor: el usuarioId del token. */
    public RutaEntidad crearEntidad(UUID creadoPor) {
        return RutaEntidad.registrar(sucursalOrigenId, sucursalDestinoId, distanciaKm, precioBoleto, creadoPor);
    }
}