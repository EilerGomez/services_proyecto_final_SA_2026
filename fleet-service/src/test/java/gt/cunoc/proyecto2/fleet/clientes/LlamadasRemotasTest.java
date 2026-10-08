package gt.cunoc.proyecto2.fleet.clientes;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.fleet.VerificarError;
import gt.cunoc.proyecto2.fleet.errores.integraciones.ErrorIntegracion;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;

class LlamadasRemotasTest {

    private final CircuitBreaker circuito = CircuitBreaker.ofDefaults("trips");

    @Test
    void devuelveLaRespuesta() {
        assertThat(LlamadasRemotas.ejecutar(circuito, () -> "ok", ErrorIntegracion.TRIPS_NO_DISPONIBLE))
                .isEqualTo("ok");
    }

    @Test
    void unaRespuestaSinCuerpoEsServicioNoDisponible() {
        VerificarError.lanza(() -> LlamadasRemotas.ejecutar(circuito, () -> null,
                ErrorIntegracion.TRIPS_NO_DISPONIBLE), ErrorIntegracion.TRIPS_NO_DISPONIBLE);
    }

    @Test
    void unErrorDelServidorEsServicioNoDisponible() {
        VerificarError.lanza(() -> LlamadasRemotas.ejecutar(circuito, () -> {
            throw new HttpServerErrorException(HttpStatus.BAD_GATEWAY);
        }, ErrorIntegracion.TRIPS_NO_DISPONIBLE), ErrorIntegracion.TRIPS_NO_DISPONIBLE);
    }

    @Test
    void un4xxSinCuerpoLegibleEsServicioNoDisponible() {
        VerificarError.lanza(() -> LlamadasRemotas.ejecutar(circuito, () -> {
            throw new HttpClientErrorException(HttpStatus.CONFLICT);
        }, ErrorIntegracion.IDENTITY_NO_DISPONIBLE), ErrorIntegracion.IDENTITY_NO_DISPONIBLE);
    }

    @Test
    void conElCircuitoAbiertoNiSiquieraLlama() {
        AtomicInteger llamadas = new AtomicInteger();
        circuito.transitionToOpenState();

        VerificarError.lanza(() -> LlamadasRemotas.ejecutar(circuito, () -> {
            llamadas.incrementAndGet();
            return "ok";
        }, ErrorIntegracion.TRIPS_NO_DISPONIBLE), ErrorIntegracion.TRIPS_NO_DISPONIBLE);
        assertThat(llamadas).hasValue(0);
    }
}
