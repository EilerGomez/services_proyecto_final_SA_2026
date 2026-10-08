package gt.cunoc.proyecto2.fleet.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.cliente.ClientesInternos;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClient;

class ConfiguracionClientesTest {

    private final ConfiguracionClientes configuracion = new ConfiguracionClientes();

    @Test
    void cadaClienteUsaLaUrlDeSuServicio() {
        ClientesInternos clientes = mock(ClientesInternos.class);
        RestClient identity = RestClient.create();
        RestClient trips = RestClient.create();
        when(clientes.crear("http://identity-service:8081")).thenReturn(identity);
        when(clientes.crear("http://trips-service:8083")).thenReturn(trips);

        assertThat(configuracion.clienteRestIdentity(clientes, "http://identity-service:8081")).isSameAs(identity);
        assertThat(configuracion.clienteRestTrips(clientes, "http://trips-service:8083")).isSameAs(trips);
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
    }

    @Test
    void esquemaDeSeguridadDeSwagger() {
        assertThat(new ConfiguracionOpenApi()).isNotNull();
        assertThat(ConfiguracionOpenApi.ESQUEMA_JWT).isEqualTo("jwt");
    }
}
