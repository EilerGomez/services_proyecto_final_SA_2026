package gt.cunoc.proyecto2.wallet.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
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
        return CircuitBreakerRegistry.of(configuracion());
    }
}
