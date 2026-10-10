package gt.cunoc.proyecto2.sales.configuracion;

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

    public static final String CLIENTE_TRIPS = "clienteRestTrips";

    @Bean(CLIENTE_TRIPS)
    public RestClient clienteRestTrips(ClientesInternos clientes,
                                       @Value("${app.servicios.trips-url:http://localhost:8083}") String url) {
        return clientes.crear(url);
    }

    /*
     * Circuitos de Resilience4j (uno por servicio: "trips").
     * Se abre si fallan 50 % de las últimas 10 llamadas (mínimo 5) y se queda abierto 30 s.
     * Los 4xx no cuentan como falla: el otro servicio respondió bien, el dato era inválido.
     */
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