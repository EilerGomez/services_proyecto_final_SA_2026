package gt.cunoc.proyecto2.fleet.eventos.recibidos.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Payload de ViajeIniciado (de Trips), solo con los campos que usa Fleet. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViajeIniciado(
        UUID viajeId,
        UUID busId,
        OffsetDateTime fechaHoraSalida) {
}