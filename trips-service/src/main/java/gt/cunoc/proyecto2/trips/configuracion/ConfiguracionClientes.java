package gt.cunoc.proyecto2.trips.configuracion;

import gt.cunoc.proyecto2.comun.cliente.ClientesInternos;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Configuration
public class ConfiguracionClientes {

    public static final String CLIENTE_FLEET = "clienteRestFleet";

    /** R1a y R1b. Token de servicio, correlación y espera de 3 s los agrega ClientesInternos. */
    @Bean(CLIENTE_FLEET)
    public RestClient clienteRestFleet(ClientesInternos clientes,
                                       @Value("${app.servicios.fleet-url:http://localhost:8082}") String url) {
        return clientes.crear(url);
    }

    @Bean
    public CircuitBreakerRegistry registroCircuitos() {
        CircuitBreakerConfig configuracion = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(2)
                .ignoreExceptions(HttpClientErrorException.class)
                .build();
        return CircuitBreakerRegistry.of(configuracion);
    }
}