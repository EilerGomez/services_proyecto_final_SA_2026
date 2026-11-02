package api_gateway.revocaciones;

import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;
import tools.jackson.databind.JsonNode;

/**
 * Al iniciar trae de identity las revocaciones de la última hora por medio de una solicitud
 * interna para saber las que ocurrieron mientras el gateway estaba apagado.
 *
 * Si identity aún no responde lo reintenta en segundo plano, el gateway atiende mientras tanto.
 */
@Component
@ConditionalOnProperty(prefix = "gateway.revocaciones", name = "enabled", havingValue = "true", matchIfMissing = true)
public class CargaInicialRevocaciones {

    static final String RUTA = "/identity/interno/sesiones-revocadas";

    private static final Logger log = LoggerFactory.getLogger(CargaInicialRevocaciones.class);

    private final WebClient identity;
    private final TokenServicio token;
    private final RegistroRevocaciones registro;
    private final int intentos;
    private final Duration espera;

    // Prepara el cliente HTTP y parámetros para recuperar revocaciones desde identity.
    @Autowired
    public CargaInicialRevocaciones(@Value("${gateway.services.identity-url}") String identityUrl, TokenServicio token, RegistroRevocaciones registro) {
        this(WebClient.create(identityUrl), token, registro, 40, Duration.ofSeconds(3));
    }

    // Permite inyectar cliente, token y parámetros de reintento, también en las pruebas.
    CargaInicialRevocaciones(WebClient identity, TokenServicio token, RegistroRevocaciones registro, int intentos, Duration espera) {
        this.identity = identity;
        this.token = token;
        this.registro = registro;
        this.intentos = intentos;
        this.espera = espera;
    }

    // Inicia la recuperación de revocaciones al estar lista la aplicación.
    @EventListener(ApplicationReadyEvent.class)
    public void alArrancar() {
        cargar().subscribe(
                cantidad -> log.info("Revocaciones de sesión cargadas de identity: {}", cantidad),
                error -> log.warn("No se pudieron cargar las revocaciones de identity: {}", error.getMessage()));
    }

    // Devuelve cuántas revocaciones se registraron.
    Mono<Integer> cargar() {
        return Mono.defer(() -> identity.get().uri(RUTA)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token.generar())
                        .retrieve()
                        .bodyToFlux(JsonNode.class)
                        .filter(nodo -> LectorRevocacion.registrar(nodo, registro))
                        .count()
                        .map(Long::intValue))
                .retryWhen(Retry.fixedDelay(intentos, espera));
    }
}
