package gt.cunoc.proyecto2.trips.dtos.alquileres;

import gt.cunoc.proyecto2.trips.modelos.alquileres.ConfiguracionAlquilerEntidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Respuesta de T1 y T2. */
public record ConfiguracionAlquilerRespuesta(
        UUID configuracionId,
        BigDecimal precioPorKm,
        LocalDate vigenteDesde,
        UUID creadoPor) {

    public ConfiguracionAlquilerRespuesta(ConfiguracionAlquilerEntidad configuracion) {
        this(configuracion.getId(),
                configuracion.getPrecioPorKm(),
                configuracion.getVigenteDesde(),
                configuracion.getCreadoPor());
    }
}