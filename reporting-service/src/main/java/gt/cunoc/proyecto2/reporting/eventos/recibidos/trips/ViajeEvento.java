package gt.cunoc.proyecto2.reporting.eventos.recibidos.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Payload de ViajeProgramado y ViajeActualizado (mismo contenido). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViajeEvento(
        UUID viajeId,
        String tipo,
        UUID rutaId,
        UUID sucursalId,
        UUID busId,
        UUID choferId,
        OffsetDateTime fechaSalida) {
}
