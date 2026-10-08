package gt.cunoc.proyecto2.fleet.eventos.recibidos.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Payload de ViajeCompletado (de Trips), solo con los campos que usa Fleet. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViajeCompletado(
        UUID viajeId,
        UUID busId,
        BigDecimal kmInicial,
        BigDecimal kmFinal,
        OffsetDateTime fechaHoraLlegada) {
}