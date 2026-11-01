package gt.cunoc.proyecto2.fleet.configuracion;

import gt.cunoc.proyecto2.comun.cliente.ClientesInternos;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/** Clientes REST hacia otros servicios (R3, R5) y la configuración de sus circuitos. */
@Configuration
public class ConfiguracionClientes {

    private static final Logger log = LoggerFactory.getLogger(ConfiguracionClientes.class);

    public static final String CLIENTE_IDENTITY = "clienteRestIdentity";
    public static final String CLIENTE_TRIPS = "clienteRestTrips";

    /** R5. Token de servicio, correlación y espera de 3 s los agrega ClientesInternos. */
    @Bean(CLIENTE_IDENTITY)
    public RestClient clienteRestIdentity(ClientesInternos clientes,
                                          @Value("${app.servicios.identity-url:http://localhost:8081}") String url) {
        return clientes.crear(url);
    }

    /** R3. */
    @Bean(CLIENTE_TRIPS)
    public RestClient clienteRestTrips(ClientesInternos clientes,
                                       @Value("${app.servicios.trips-url:http://localhost:8083}") String url) {
        return clientes.crear(url);
    }

    /**
     * Circuitos de Resilience4j (uno por servicio: "identity", "trips").
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
        CircuitBreakerRegistry registro = CircuitBreakerRegistry.of(configuracion);
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
