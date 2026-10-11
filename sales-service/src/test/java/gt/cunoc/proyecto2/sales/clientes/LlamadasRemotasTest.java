package gt.cunoc.proyecto2.sales.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gt.cunoc.proyecto2.comun.errores.ErrorRespuesta;
import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.comun.errores.ServicioNoDisponibleException;
import gt.cunoc.proyecto2.sales.VerificarError;
import gt.cunoc.proyecto2.sales.errores.integraciones.ErrorIntegracion;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

class LlamadasRemotasTest {

    private final CircuitBreaker circuito = CircuitBreaker.ofDefaults("trips");

    /** Un 4xx como lo arma RestClient, con la función que convierte el cuerpo. */
    private static HttpClientErrorException error(HttpStatus status, String codigo) {
        HttpClientErrorException e = HttpClientErrorException.create(status, status.getReasonPhrase(),
                new HttpHeaders(), new byte[0], null);
        ErrorRespuesta cuerpo = new ErrorRespuesta(status.value(), codigo, "Mensaje de Trips",
                "/trips/interno/viajes/x/vendible", Instant.now(), null, List.of());
        e.setBodyConvertFunction(tipo -> cuerpo);
        return e;
    }

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
        assertThatThrownBy(() -> LlamadasRemotas.ejecutar(circuito, () -> {
            throw new HttpServerErrorException(HttpStatus.BAD_GATEWAY);
        }, ErrorIntegracion.TRIPS_NO_DISPONIBLE)).isInstanceOfSatisfying(ServicioNoDisponibleException.class, e -> {
            assertThat(e.getCodigo()).isEqualTo("TRIPS_NO_DISPONIBLE");
            assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        });
        assertThat(circuito.getMetrics().getNumberOfFailedCalls()).isEqualTo(1);
    }

    @Test
    void unErrorDeRedSeReintentaYTerminaEnServicioNoDisponible() {
        AtomicInteger intentos = new AtomicInteger();

        VerificarError.lanza(() -> LlamadasRemotas.ejecutar(circuito, () -> {
            intentos.incrementAndGet();
            throw new ResourceAccessException("sin red");
        }, ErrorIntegracion.TRIPS_NO_DISPONIBLE), ErrorIntegracion.TRIPS_NO_DISPONIBLE);
        assertThat(intentos).hasValue(Reintentos.INTENTOS);
    }

    @Test
    void un4xxDelUsuarioPasaConElMismoCodigo() {
        assertThatThrownBy(() -> LlamadasRemotas.ejecutar(circuito, () -> {
            throw error(HttpStatus.valueOf(422), "FECHA_INVALIDA");
        }, ErrorIntegracion.TRIPS_NO_DISPONIBLE)).isInstanceOfSatisfying(NegocioException.class, e -> {
            assertThat(e.getCodigo()).isEqualTo("FECHA_INVALIDA");
            assertThat(e.getStatus().value()).isEqualTo(422);
            assertThat(e.getMessage()).isEqualTo("Mensaje de Trips");
        });
    }

    @Test
    void un404DeTripsEsServicioNoDisponible() {
        VerificarError.lanza(() -> LlamadasRemotas.ejecutar(circuito, () -> {
            throw error(HttpStatus.NOT_FOUND, "RUTA_INEXISTENTE");
        }, ErrorIntegracion.TRIPS_NO_DISPONIBLE), ErrorIntegracion.TRIPS_NO_DISPONIBLE);
    }

    @Test
    void un4xxSinCuerpoLegibleEsServicioNoDisponible() {
        VerificarError.lanza(() -> LlamadasRemotas.ejecutar(circuito, () -> {
            throw new HttpClientErrorException(HttpStatus.CONFLICT);
        }, ErrorIntegracion.TRIPS_NO_DISPONIBLE), ErrorIntegracion.TRIPS_NO_DISPONIBLE);
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
