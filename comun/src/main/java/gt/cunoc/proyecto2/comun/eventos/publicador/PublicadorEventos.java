package gt.cunoc.proyecto2.comun.eventos.publicador;

import gt.cunoc.proyecto2.comun.eventos.sobre.EventoSobre;
import gt.cunoc.proyecto2.comun.eventos.contextocorrelacion.ContextoCorrelacion;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Transactional Outbox: guarda el evento en {@code eventos_salida} dentro de la MISMA transacción
 * del cambio de negocio. {@link RelayOutbox} lo publica después en RabbitMQ.
 * Así, si el cambio se guardó, el evento se publicará tarde o temprano, y viceversa.
 *
 * <p>Se exige una transacción activa (Propagation.MANDATORY): llamar desde un método @Transactional.</p>
 */
@Component
@ConditionalOnProperty(prefix = "comun.eventos", name = "exchange")
public class PublicadorEventos {

    private static final String INSERTAR = """
            INSERT INTO eventos_salida (id, tipo_agregado, agregado_id, tipo_evento, version_evento, datos)
            VALUES (?, ?, ?, ?, ?, CAST(? AS jsonb))
            """;

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    public PublicadorEventos(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    /**
     * @param tipoAgregado  p. ej. "Venta", "Viaje"
     * @param agregadoId    id principal (va como aggregateId)
     * @param tipoEvento    p. ej. "TicketSold" (es la routing key)
     * @param payload       record con los datos del catálogo de eventos
     * @param fechaNegocio  fecha elegida por el usuario
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public EventoSobre publicar(String tipoAgregado, UUID agregadoId, String tipoEvento,
                                Object payload, LocalDate fechaNegocio) {
        return guardar(tipoAgregado, agregadoId, null, tipoEvento, payload, fechaNegocio);
    }

    /** Igual, indicando la versión del agregado para que los consumidores descarten eventos viejos. */
    @Transactional(propagation = Propagation.MANDATORY)
    public EventoSobre publicar(String tipoAgregado, UUID agregadoId, Long versionAgregado, String tipoEvento,
                                Object payload, LocalDate fechaNegocio) {
        return guardar(tipoAgregado, agregadoId, versionAgregado, tipoEvento, payload, fechaNegocio);
    }

    private EventoSobre guardar(String tipoAgregado, UUID agregadoId, Long versionAgregado, String tipoEvento,
                                Object payload, LocalDate fechaNegocio) {
        Objects.requireNonNull(tipoAgregado, "tipoAgregado");
        Objects.requireNonNull(agregadoId, "agregadoId");
        Objects.requireNonNull(tipoEvento, "tipoEvento");
        Objects.requireNonNull(payload, "payload");

        EventoSobre sobre = new EventoSobre(
                UUID.randomUUID(),
                tipoEvento,
                1,
                agregadoId,
                versionAgregado,
                Instant.now(),
                fechaNegocio != null ? fechaNegocio : LocalDate.now(),
                ContextoCorrelacion.actualOGenerar(),
                payload);

        jdbc.update(INSERTAR, sobre.eventId(), tipoAgregado, agregadoId, tipoEvento, sobre.version(),
                json.writeValueAsString(sobre));
        return sobre;
    }
}
