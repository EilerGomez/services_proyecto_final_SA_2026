package api_gateway.revocaciones;

import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Escucha SesionRevocada (sobre de comun: {@code {eventType, payload: {usuarioId, tokensValidosDesde}}})
 * y lo pasa al registro: desde ese momento el gateway rechaza los JWT viejos de esa persona.
 * Un mensaje que no se puede leer se registra en el log y se descarta (sin reintentos infinitos).
 */
@Component
@ConditionalOnProperty(prefix = "gateway.revocaciones", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OyenteSesionRevocada {

    private static final Logger log = LoggerFactory.getLogger(OyenteSesionRevocada.class);

    private final RegistroRevocaciones registro;
    private final ObjectMapper json;

    public OyenteSesionRevocada(RegistroRevocaciones registro, ObjectMapper json) {
        this.registro = registro;
        this.json = json;
    }

    @RabbitListener(queues = "#{colaRevocaciones.name}")
    public void recibir(Message mensaje) {
        try {
            JsonNode sobre = json.readTree(new String(mensaje.getBody(), StandardCharsets.UTF_8));
            JsonNode payload = sobre.path("payload");
            if (LectorRevocacion.registrar(payload, registro)) {
                log.info("Sesiones revocadas del usuario {}", payload.path("usuarioId").asText());
            } else {
                log.warn("SesionRevocada sin usuarioId o tokensValidosDesde: se ignora");
            }
        } catch (RuntimeException e) {
            log.warn("No se pudo leer SesionRevocada: {}", e.getMessage());
        }
    }
}
