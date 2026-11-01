package gt.cunoc.proyecto2.notification.configuracion;

import gt.cunoc.proyecto2.comun.cliente.ClientesInternos;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

/*
 * Cliente REST hacia Identity (R4): administradores de una sucursal y contacto de un usuario.
 * El token de servicio, la correlación y la espera de 3 s los agrega ClientesInternos.
 */
@Configuration
public class ConfiguracionClientes {

    public static final String CLIENTE_IDENTITY = "clienteRestIdentity";

    @Bean(CLIENTE_IDENTITY)
    public RestClient clienteRestIdentity(ClientesInternos clientes,
                                          @Value("${app.servicios.identity-url:http://localhost:8081}") String url) {
        return clientes.crear(url);
    }

    /**
     * Misma configuración que trips, sales y fleet: con Identity caído se corta la llamada en vez de
     * esperar los 3 s de cada intento. Un 4xx no cuenta como falla del servicio, porque es una
     * respuesta válida (la cuenta no existe), no una caída.
     */
    public static CircuitBreakerConfig configuracionDeCircuitos() {
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
        return CircuitBreakerRegistry.of(configuracionDeCircuitos());
    }
}
