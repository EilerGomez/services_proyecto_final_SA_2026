package gt.cunoc.proyecto2.trips.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.cliente.ClientesInternos;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

class ConfiguracionClientesTest {

    private final ConfiguracionClientes configuracion = new ConfiguracionClientes();

    @Test
    void elClienteDeFleetUsaLaUrlDeFleet() {
        ClientesInternos clientes = mock(ClientesInternos.class);
        RestClient fleet = RestClient.create();
        when(clientes.crear("http://fleet-service:8082")).thenReturn(fleet);

        assertThat(configuracion.clienteRestFleet(clientes, "http://fleet-service:8082")).isSameAs(fleet);
        assertThat(ConfiguracionClientes.CLIENTE_FLEET).isEqualTo("clienteRestFleet");
    }

    @Test
    void elCircuitoIgnoraLos4xx() {
        CircuitBreakerConfig circuito = configuracion.registroCircuitos().getDefaultConfig();

        assertThat(circuito.getSlidingWindowSize()).isEqualTo(10);
        assertThat(circuito.getMinimumNumberOfCalls()).isEqualTo(5);
        assertThat(circuito.getFailureRateThreshold()).isEqualTo(50f);
        assertThat(circuito.getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(2);
        assertThat(circuito.getIgnoreExceptionPredicate().test(new HttpClientErrorException(HttpStatus.CONFLICT)))
                .isTrue();
        assertThat(circuito.getIgnoreExceptionPredicate()
                .test(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))).isFalse();
        assertThat(circuito.getIgnoreExceptionPredicate().test(new ResourceAccessException("sin red"))).isFalse();
    }

    @Test
    void elCircuitoDeFleetUsaLaConfiguracionPorDefecto() {
        CircuitBreakerRegistry registro = configuracion.registroCircuitos();

        CircuitBreakerConfig fleet = registro.circuitBreaker("fleet").getCircuitBreakerConfig();

        assertThat(fleet.getSlidingWindowSize()).isEqualTo(10);
        assertThat(fleet.getMinimumNumberOfCalls()).isEqualTo(5);
    }

    @Test
    void esquemaDeSeguridadDeSwagger() {
        assertThat(new ConfiguracionOpenApi()).isNotNull();
        assertThat(ConfiguracionOpenApi.ESQUEMA_JWT).isEqualTo("jwt");
    }
}
