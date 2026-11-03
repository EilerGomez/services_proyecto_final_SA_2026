package gt.cunoc.proyecto2.comun.eventos.consumidor;

import gt.cunoc.proyecto2.comun.eventos.contextocorrelacion.ContextoCorrelacion;
import gt.cunoc.proyecto2.comun.eventos.recibido.EventoRecibido;
import gt.cunoc.proyecto2.comun.eventos.sobre.EventoSobre;
import gt.cunoc.proyecto2.comun.eventos.manejadorevento.ManejadorEvento;
import gt.cunoc.proyecto2.comun.idempotencia.ProcesadorIdempotente;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
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

/*
 * Único listener de la cola del servicio ({@code comun.eventos.cola}).
 * Lee el sobre, busca el {@link ManejadorEvento} del tipo de evento y lo ejecuta
 * dentro de {ProcesadorIdempotente} (un evento duplicado se ignora).
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

        ManejadorEvento<?> manejador = manejadores.get(sobre.eventType());//busca en el directorio quien maneja ese mensaje
        if (manejador == null) {
            log.warn("SAGA corr={} evento={} id={} paso=sin-manejador; se descarta",
                    sobre.correlationId(), sobre.eventType(), sobre.eventId());
            return;
        }

        UUID correlacion = sobre.correlationId() != null ? sobre.correlationId() : UUID.randomUUID();
        ContextoCorrelacion.establecer(correlacion);
        long inicio = System.nanoTime();
        try {
            boolean procesado = idempotente.procesar(sobre.eventId(), sobre.eventType(),
                    () -> despachar(manejador, sobre));
            if (procesado) {
                log.info("SAGA corr={} evento={} id={} paso=procesado manejador={} ms={}",
                        correlacion, sobre.eventType(), sobre.eventId(),
                        manejador.getClass().getSimpleName(),
                        TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio));
            } else {
                log.info("SAGA corr={} evento={} id={} paso=duplicado-ignorado",
                        correlacion, sobre.eventType(), sobre.eventId());
            }
        } catch (RuntimeException falla) {
            log.warn("SAGA corr={} evento={} id={} paso=fallo-al-procesar manejador={} causa={}",
                    correlacion, sobre.eventType(), sobre.eventId(),
                    manejador.getClass().getSimpleName(), falla.toString());
            throw falla;
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

    private <T> void despachar(ManejadorEvento<T> manejador, EventoSobre sobre) {//Convierte el payload al record que el manejador declaro
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
