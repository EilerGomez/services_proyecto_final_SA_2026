package gt.cunoc.proyecto2.comun.eventos;

import gt.cunoc.proyecto2.comun.idempotencia.ProcesadorIdempotente;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Único listener de la cola del servicio ({@code comun.eventos.cola}).
 * Lee el sobre, busca el {@link ManejadorEvento} del tipo de evento y lo ejecuta
 * dentro de {@link ProcesadorIdempotente} (un evento duplicado se ignora).
 *
 * <ul>
 *   <li>JSON inválido o payload que no se puede convertir → directo a la DLQ, sin reintentos.</li>
 *   <li>Excepción del manejador → reintentos (spring.rabbitmq.listener.simple.retry) y luego DLQ.</li>
 *   <li>Evento sin manejador → se registra en el log y se descarta.</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(prefix = "comun.eventos", name = "cola")
public class ConsumidorEventos {

    private static final Logger log = LoggerFactory.getLogger(ConsumidorEventos.class);

    private final Map<String, ManejadorEvento<?>> manejadores = new HashMap<>();
    private final ProcesadorIdempotente idempotente;
    private final JsonMapper json;

    public ConsumidorEventos(ObjectProvider<ManejadorEvento<?>> manejadores,
                             ProcesadorIdempotente idempotente, JsonMapper json) {
        this.idempotente = idempotente;
        this.json = json;
        manejadores.orderedStream().forEach(m -> {
            if (this.manejadores.putIfAbsent(m.tipoEvento(), m) != null) {
                throw new IllegalStateException("Hay dos manejadores para el evento " + m.tipoEvento());
            }
        });
        log.info("Manejadores de eventos registrados: {}", this.manejadores.keySet());
    }

    @RabbitListener(queues = "${comun.eventos.cola}")
    public void recibir(Message mensaje) {
        EventoSobre sobre = leerSobre(mensaje);

        ManejadorEvento<?> manejador = manejadores.get(sobre.eventType());
        if (manejador == null) {
            log.warn("No hay manejador para el evento {} ({}); se descarta", sobre.eventType(), sobre.eventId());
            return;
        }

        ContextoCorrelacion.establecer(sobre.correlationId() != null ? sobre.correlationId() : UUID.randomUUID());
        try {
            boolean procesado = idempotente.procesar(sobre.eventId(), sobre.eventType(),
                    () -> despachar(manejador, sobre));
            if (!procesado) {
                log.info("Evento duplicado ignorado: {} {}", sobre.eventType(), sobre.eventId());
            }
        } finally {
            ContextoCorrelacion.limpiar();
        }
    }

    private EventoSobre leerSobre(Message mensaje) {
        try {
            EventoSobre sobre = json.readValue(mensaje.getBody(), EventoSobre.class);
            if (sobre.eventId() == null || sobre.eventType() == null) {
                throw new AmqpRejectAndDontRequeueException("Evento sin eventId o eventType");
            }
            return sobre;
        } catch (JacksonException e) {
            throw new AmqpRejectAndDontRequeueException("Evento con JSON inválido", e);
        }
    }

    private <T> void despachar(ManejadorEvento<T> manejador, EventoSobre sobre) {
        T payload;
        try {
            payload = json.convertValue(sobre.payload(), manejador.tipoPayload());
        } catch (IllegalArgumentException | JacksonException e) {
            throw new AmqpRejectAndDontRequeueException(
                    "Payload de " + sobre.eventType() + " incompatible con " + manejador.tipoPayload().getSimpleName(), e);
        }
        manejador.manejar(new EventoRecibido<>(
                sobre.eventId(), sobre.eventType(), sobre.version(), sobre.aggregateId(),
                sobre.aggregateVersion(), sobre.occurredAt(), sobre.businessDate(),
                sobre.correlationId(), payload));
    }
}
