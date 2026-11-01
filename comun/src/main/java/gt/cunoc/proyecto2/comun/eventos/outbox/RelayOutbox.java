package gt.cunoc.proyecto2.comun.eventos.outbox;

import gt.cunoc.proyecto2.comun.eventos.contextocorrelacion.ContextoCorrelacion;
import gt.cunoc.proyecto2.comun.eventos.propiedadesevento.PropiedadesEventos;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Publica en RabbitMQ los eventos pendientes de {@code eventos_salida}, en orden de creación.
 * Un evento solo se marca como publicado cuando el broker lo confirma (publisher confirms),
 * por eso el servicio necesita {@code spring.rabbitmq.publisher-confirm-type: correlated}.
 * Si uno falla, se detiene el lote para no desordenar los eventos y se reintenta en el siguiente ciclo.
 * {@code FOR UPDATE SKIP LOCKED} permite correr varias instancias del servicio sin publicar dos veces.
 */
@Component
@ConditionalOnProperty(prefix = "comun.eventos", name = "exchange")
public class RelayOutbox {

    private static final Logger log = LoggerFactory.getLogger(RelayOutbox.class);
    private static final long ESPERA_CONFIRMACION_SEG = 5;

    /* El correlationId se saca del propio sobre guardado en datos: este hilo es el del @Scheduled
       y no tiene el id en el MDC, así que se recupera de la fila para poder seguir la saga. */
    private static final String PENDIENTES = """
            SELECT id, tipo_evento, datos::text AS datos, (datos->>'correlationId')::uuid AS correlacion
            FROM eventos_salida
            WHERE fecha_publicacion IS NULL
            ORDER BY fecha_creacion
            LIMIT ?
            FOR UPDATE SKIP LOCKED
            """;
    private static final String MARCAR_PUBLICADO = """
            UPDATE eventos_salida
            SET fecha_publicacion = now(), intentos = intentos + 1, ultimo_error = NULL
            WHERE id = ?
            """;
    private static final String MARCAR_FALLO = """
            UPDATE eventos_salida SET intentos = intentos + 1, ultimo_error = ? WHERE id = ?
            """;

    private final JdbcTemplate jdbc;
    private final RabbitTemplate rabbit;
    private final TransactionTemplate transaccion;
    private final PropiedadesEventos propiedades;

    public RelayOutbox(JdbcTemplate jdbc, RabbitTemplate rabbit,
                       PlatformTransactionManager transactionManager, PropiedadesEventos propiedades) {
        this.jdbc = jdbc;
        this.rabbit = rabbit;
        this.transaccion = new TransactionTemplate(transactionManager);
        this.propiedades = propiedades;
    }

    @Scheduled(fixedDelayString = "${comun.eventos.outbox.intervalo-ms:1000}")
    public void publicarPendientes() {
        transaccion.executeWithoutResult(estado -> {
            List<Pendiente> pendientes = jdbc.query(PENDIENTES,
                    (rs, fila) -> new Pendiente(
                            rs.getObject("id", UUID.class),
                            rs.getString("tipo_evento"),
                            rs.getString("datos"),
                            rs.getObject("correlacion", UUID.class)),
                    propiedades.outbox().lote());

            for (Pendiente pendiente : pendientes) {
                if (!publicar(pendiente)) {
                    break;
                }
            }
        });
    }

    /** Deja el id de correlación en el MDC mientras se publica, para que las líneas lo lleven. */
    private boolean publicar(Pendiente pendiente) {
        if (pendiente.correlacion() != null) {
            ContextoCorrelacion.establecer(pendiente.correlacion());
        }
        try {
            return enviar(pendiente);
        } finally {
            ContextoCorrelacion.limpiar();
        }
    }

    private boolean enviar(Pendiente pendiente) {
        try {
            Message mensaje = MessageBuilder.withBody(pendiente.datos().getBytes(StandardCharsets.UTF_8))
                    .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                    .setContentEncoding(StandardCharsets.UTF_8.name())
                    .setMessageId(pendiente.id().toString())
                    .setType(pendiente.tipoEvento())
                    .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                    .build();

            CorrelationData confirmacion = new CorrelationData(pendiente.id().toString());
            rabbit.send(propiedades.exchange(), pendiente.tipoEvento(), mensaje, confirmacion);

            CorrelationData.Confirm resultado = confirmacion.getFuture()
                    .get(ESPERA_CONFIRMACION_SEG, TimeUnit.SECONDS);
            if (!resultado.ack()) {
                throw new IllegalStateException("RabbitMQ rechazó el evento: " + resultado.reason());
            }

            jdbc.update(MARCAR_PUBLICADO, pendiente.id());
            log.info("SAGA corr={} evento={} id={} paso=publicado exchange={}",
                    pendiente.correlacion(), pendiente.tipoEvento(), pendiente.id(), propiedades.exchange());
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            registrarFallo(pendiente, e);
            return false;
        } catch (Exception e) {
            registrarFallo(pendiente, e);
            return false;
        }
    }

    private void registrarFallo(Pendiente pendiente, Exception e) {
        log.warn("SAGA corr={} evento={} id={} paso=fallo-al-publicar causa={}",
                pendiente.correlacion(), pendiente.tipoEvento(), pendiente.id(), e.toString());
        String error = e.toString();
        jdbc.update(MARCAR_FALLO, error.length() > 1000 ? error.substring(0, 1000) : error, pendiente.id());
    }

    private record Pendiente(UUID id, String tipoEvento, String datos, UUID correlacion) {
    }
}
