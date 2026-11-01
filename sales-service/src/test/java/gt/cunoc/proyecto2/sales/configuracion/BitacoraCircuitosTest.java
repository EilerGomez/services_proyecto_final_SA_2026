package gt.cunoc.proyecto2.sales.configuracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.Test;

/**
 * La bitácora de los circuitos solo escribe en el log, pero se prueba igual: si una de esas lambdas
 * tronara, lo haría dentro de una llamada real en producción, que es el peor momento para enterarse.
 */
class BitacoraCircuitosTest {

    @Test
    void registraLaTransicionDeEstadoYLaLlamadaCortada() {
        // Arrange: el registro del servicio ya viene con la bitácora enganchada
        CircuitBreakerRegistry registro = new ConfiguracionClientes().registroCircuitos();

        // Act: crear el circuito dispara onEntryAdded; abrirlo, onStateTransition
        CircuitBreaker circuito = registro.circuitBreaker("trips");
        circuito.transitionToOpenState();

        // Assert: y con el circuito abierto, la llamada se corta (onCallNotPermitted)
        assertThatThrownBy(() -> circuito.executeSupplier(() -> "no deberia ejecutarse"))
                .isInstanceOf(CallNotPermittedException.class);
        assertThat(circuito.getState()).isEqualTo(CircuitBreaker.State.OPEN);
    }

    @Test
    void laConfiguracionEsLaMismaQueEnLosDemasServicios() {
        CircuitBreaker circuito = new ConfiguracionClientes().registroCircuitos().circuitBreaker("trips");

        assertThat(circuito.getCircuitBreakerConfig().getSlidingWindowSize()).isEqualTo(10);
        assertThat(circuito.getCircuitBreakerConfig().getMinimumNumberOfCalls()).isEqualTo(5);
        assertThat(circuito.getCircuitBreakerConfig().getFailureRateThreshold()).isEqualTo(50f);
        assertThat(circuito.getCircuitBreakerConfig().getPermittedNumberOfCallsInHalfOpenState()).isEqualTo(2);
    }
}
