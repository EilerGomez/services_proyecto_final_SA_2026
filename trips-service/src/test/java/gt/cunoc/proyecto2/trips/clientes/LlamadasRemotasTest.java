package gt.cunoc.proyecto2.trips.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import gt.cunoc.proyecto2.comun.errores.ErrorRespuesta;
import gt.cunoc.proyecto2.comun.errores.NegocioException;
import gt.cunoc.proyecto2.comun.errores.ServicioNoDisponibleException;
import gt.cunoc.proyecto2.trips.VerificarError;
import gt.cunoc.proyecto2.trips.errores.integraciones.ErrorIntegracion;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

class LlamadasRemotasTest {

    private static final Set<String> BUS_NO_EXISTE = Set.of("BUS_NO_EXISTE");

    private final CircuitBreaker circuito = CircuitBreaker.ofDefaults("fleet");

    /** Un 4xx como lo arma RestClient (404 → subclase NotFound), con la función que convierte el cuerpo. */
    private static HttpClientErrorException error(HttpStatus status, String codigo) {
        HttpClientErrorException e = HttpClientErrorException.create(status, status.getReasonPhrase(),
                new HttpHeaders(), new byte[0], null);
        ErrorRespuesta cuerpo = new ErrorRespuesta(status.value(), codigo, "Mensaje de Fleet",
                "/fleet/interno/buses/x/disponibilidad", Instant.now(), null, List.of());
        e.setBodyConvertFunction(tipo -> cuerpo);
        return e;
    }

    // ----- ejecutar -----

    @Test
    void devuelveLaRespuesta() {
        assertThat(LlamadasRemotas.ejecutar(circuito, () -> "ok", ErrorIntegracion.FLEET_NO_DISPONIBLE))
                .isEqualTo("ok");
    }

    @Test
    void unaRespuestaSinCuerpoEsServicioNoDisponible() {
        VerificarError.lanza(() -> LlamadasRemotas.ejecutar(circuito, () -> null,
                ErrorIntegracion.FLEET_NO_DISPONIBLE), ErrorIntegracion.FLEET_NO_DISPONIBLE);
    }

    @Test
    void unErrorDelServidorEsServicioNoDisponible() {
        assertThatThrownBy(() -> LlamadasRemotas.ejecutar(circuito, () -> {
            throw new HttpServerErrorException(HttpStatus.BAD_GATEWAY);
        }, ErrorIntegracion.FLEET_NO_DISPONIBLE)).isInstanceOfSatisfying(ServicioNoDisponibleException.class, e -> {
            assertThat(e.getCodigo()).isEqualTo("FLEET_NO_DISPONIBLE");
            assertThat(e.getStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        });
    }

    @Test
    void unErrorDeRedSeReintentaYTerminaEnServicioNoDisponible() {
        AtomicInteger intentos = new AtomicInteger();

        VerificarError.lanza(() -> LlamadasRemotas.ejecutar(circuito, () -> {
            intentos.incrementAndGet();
            throw new ResourceAccessException("sin red");
        }, ErrorIntegracion.FLEET_NO_DISPONIBLE), ErrorIntegracion.FLEET_NO_DISPONIBLE);
        assertThat(intentos).hasValue(Reintentos.INTENTOS);
    }

    @Test
    void un4xxDelUsuarioPasaConElMismoCodigo() {
        assertThatThrownBy(() -> LlamadasRemotas.ejecutar(circuito, () -> {
            throw error(HttpStatus.valueOf(422), "BUS_EN_TALLER");
        }, ErrorIntegracion.FLEET_NO_DISPONIBLE)).isInstanceOfSatisfying(NegocioException.class, e -> {
            assertThat(e.getCodigo()).isEqualTo("BUS_EN_TALLER");
            assertThat(e.getStatus().value()).isEqualTo(422);
        });
    }

    @Test
    void un4xxSinCuerpoLegibleEsServicioNoDisponible() {
        VerificarError.lanza(() -> LlamadasRemotas.ejecutar(circuito, () -> {
            throw new HttpClientErrorException(HttpStatus.CONFLICT);
        }, ErrorIntegracion.FLEET_NO_DISPONIBLE), ErrorIntegracion.FLEET_NO_DISPONIBLE);
    }

    @Test
    void conElCircuitoAbiertoNiSiquieraLlama() {
        AtomicInteger llamadas = new AtomicInteger();
        circuito.transitionToOpenState();

        VerificarError.lanza(() -> LlamadasRemotas.ejecutar(circuito, () -> {
            llamadas.incrementAndGet();
            return "ok";
        }, ErrorIntegracion.FLEET_NO_DISPONIBLE), ErrorIntegracion.FLEET_NO_DISPONIBLE);
        assertThat(llamadas).hasValue(0);
    }

    // ----- ejecutarOpcional -----

    @Test
    void opcionalConRespuestaDevuelveElValor() {
        Optional<String> respuesta = LlamadasRemotas.ejecutarOpcional(circuito, () -> "bus",
                ErrorIntegracion.FLEET_NO_DISPONIBLE, BUS_NO_EXISTE);

        assertThat(respuesta).contains("bus");
    }

    @Test
    void un404ConCodigoDeNoExisteEsOptionalVacio() {
        Optional<String> respuesta = LlamadasRemotas.ejecutarOpcional(circuito, () -> {
            throw error(HttpStatus.NOT_FOUND, "BUS_NO_EXISTE");
        }, ErrorIntegracion.FLEET_NO_DISPONIBLE, BUS_NO_EXISTE);

        assertThat(respuesta).isEmpty();
    }

    @Test
    void un404ConOtroCodigoEsServicioNoDisponible() {
        VerificarError.lanza(() -> LlamadasRemotas.ejecutarOpcional(circuito, () -> {
            throw error(HttpStatus.NOT_FOUND, "RUTA_INEXISTENTE");
        }, ErrorIntegracion.FLEET_NO_DISPONIBLE, BUS_NO_EXISTE), ErrorIntegracion.FLEET_NO_DISPONIBLE);
    }

    @Test
    void un404SinCodigoNoRompeElSetYEsServicioNoDisponible() {
        // Set.of(...).contains(null) lanzaría NullPointerException: el código nulo se descarta antes
        VerificarError.lanza(() -> LlamadasRemotas.ejecutarOpcional(circuito, () -> {
            throw error(HttpStatus.NOT_FOUND, null);
        }, ErrorIntegracion.FLEET_NO_DISPONIBLE, BUS_NO_EXISTE), ErrorIntegracion.FLEET_NO_DISPONIBLE);
    }

    @Test
    void un404SinCuerpoLegibleEsServicioNoDisponible() {
        VerificarError.lanza(() -> LlamadasRemotas.ejecutarOpcional(circuito, () -> {
            throw HttpClientErrorException.create(HttpStatus.NOT_FOUND, "Not Found", new HttpHeaders(),
                    new byte[0], null);
        }, ErrorIntegracion.FLEET_NO_DISPONIBLE, BUS_NO_EXISTE), ErrorIntegracion.FLEET_NO_DISPONIBLE);
    }

    @Test
    void opcionalSinCuerpoEsServicioNoDisponible() {
        VerificarError.lanza(() -> LlamadasRemotas.ejecutarOpcional(circuito, () -> null,
                ErrorIntegracion.FLEET_NO_DISPONIBLE, BUS_NO_EXISTE), ErrorIntegracion.FLEET_NO_DISPONIBLE);
    }

    @Test
    void opcionalConUn422PasaElCodigoDeFleet() {
        VerificarError.lanza(() -> LlamadasRemotas.ejecutarOpcional(circuito, () -> {
            throw error(HttpStatus.valueOf(422), "BUS_INACTIVO");
        }, ErrorIntegracion.FLEET_NO_DISPONIBLE, BUS_NO_EXISTE), "BUS_INACTIVO");
    }

    @Test
    void opcionalConErrorDeRedOCircuitoAbiertoEsServicioNoDisponible() {
        VerificarError.lanza(() -> LlamadasRemotas.ejecutarOpcional(circuito, () -> {
            throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
        }, ErrorIntegracion.FLEET_NO_DISPONIBLE, BUS_NO_EXISTE), ErrorIntegracion.FLEET_NO_DISPONIBLE);

        circuito.transitionToOpenState();
        VerificarError.lanza(() -> LlamadasRemotas.ejecutarOpcional(circuito, () -> "bus",
                ErrorIntegracion.FLEET_NO_DISPONIBLE, BUS_NO_EXISTE), ErrorIntegracion.FLEET_NO_DISPONIBLE);
    }
}
