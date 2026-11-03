package gt.cunoc.proyecto2.comun.eventos.sobre;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Sobre común de todos los eventos. Es exactamente el JSON que viaja por RabbitMQ
 * y el que se guarda en la columna {eventos_salida.datos}.
 *
 * @param eventId          id unicoS del evento (idempotencia en el consumidor)
 * @param eventType        nombre del evento = routing key (p. ej. "BoletoVendido")
 * @param version          version del esquema del evento (sube solo si se quita o renombra un campo)
 * @param aggregateId      id principal del payload (ventaId, viajeId, ...)
 * @param aggregateVersion version del agregado para descartar eventos desordenados (puede ser null)
 * @param occurredAt       momento en que se genero
 * @param businessDate     fecha de negocio elegida por el usuario (los reportes filtran por esta)
 * @param correlationId    id del flujo completo (saga)
 * @param payload          datos propios del evento
 */
public record EventoSobre(
        UUID eventId,
        String eventType,
        int version,
        UUID aggregateId,
        Long aggregateVersion,
        Instant occurredAt,
        LocalDate businessDate,
        UUID correlationId,
        Object payload) {
}
