package gt.cunoc.proyecto2.fleet.dtos.buses;

import gt.cunoc.proyecto2.fleet.modelos.buses.EstadoBus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;

/**
 * Cuerpo de F14 · PATCH /fleet/buses/{id}/estado.
 * estado: ACTIVO, EN_TALLER o INACTIVO. fecha: fecha de negocio del evento.
 */
public record CambiarEstadoBusSolicitud(
        @NotNull EstadoBus estado,
        @NotNull LocalDate fecha,
        @NotNull @PositiveOrZero Integer version) {
}