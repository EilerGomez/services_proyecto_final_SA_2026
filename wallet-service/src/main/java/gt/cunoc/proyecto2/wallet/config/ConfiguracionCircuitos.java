package gt.cunoc.proyecto2.wallet.config;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpClientErrorException;

/**
 * Circuito de la única llamada REST saliente de Wallet: R4c hacia Identity.
 * Misma configuración que trips, sales y fleet, para que los cinco se expliquen igual:
 * con Identity caído se corta la llamada en vez de esperar los 3 s de cada intento, y un 4xx
 * no cuenta como falla del servicio porque es una respuesta válida (la cuenta no existe).
 */
@Configuration
public class ConfiguracionCircuitos {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionCircuitos.class);

    public static CircuitBreakerConfig configuracion() {
        return CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(2)
                .ignoreExceptions(HttpClientErrorException.class)
                .build();
    }

    @Bean
    public CircuitBreakerRegistry registroCircuitos() {
        CircuitBreakerRegistry registro = CircuitBreakerRegistry.of(configuracion());
        conBitacora(registro);
        return registro;
    }

    /**
     * Deja rastro de cada circuito en el log: las lineas "CIRCUITO" permiten ver las aperturas con
     * p2 logs | grep CIRCUITO. Se engancha al registro, asi cubre cualquier circuito que se agregue.
     */
    static void conBitacora(CircuitBreakerRegistry registro) {
        registro.getEventPublisher().onEntryAdded(agregado -> {
            CircuitBreaker circuito = agregado.getAddedEntry();
            circuito.getEventPublisher()
                    .onStateTransition(evento -> log.warn(
                            "CIRCUITO nombre={} estado={}->{} fallos={}% llamadas={}",
                            circuito.getName(),
                            evento.getStateTransition().getFromState(),
                            evento.getStateTransition().getToState(),
                            circuito.getMetrics().getFailureRate(),
                            circuito.getMetrics().getNumberOfBufferedCalls()))
                    .onCallNotPermitted(evento -> log.warn(
                            "CIRCUITO nombre={} llamada-cortada estado={}",
                            circuito.getName(), circuito.getState()));
        });
    }
}
