package gt.cunoc.proyecto2.comun.eventos;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Evento ya leído, con el payload convertido al record que declara el {@link ManejadorEvento}. */
public record EventoRecibido<T>(
        UUID eventId,
        String eventType,
        int version,
        UUID aggregateId,
        Long aggregateVersion,
        Instant occurredAt,
        LocalDate businessDate,
        UUID correlationId,
        T payload) {
}
