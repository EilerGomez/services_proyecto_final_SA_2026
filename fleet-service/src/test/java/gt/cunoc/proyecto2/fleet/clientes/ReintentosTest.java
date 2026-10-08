package gt.cunoc.proyecto2.fleet.clientes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

class ReintentosTest {

    private final AtomicInteger intentos = new AtomicInteger();

    @Test
    void siRespondeALaPrimeraNoReintenta() {
        String respuesta = Reintentos.ejecutar(() -> {
            intentos.incrementAndGet();
            return "ok";
        });

        assertThat(respuesta).isEqualTo("ok");
        assertThat(intentos).hasValue(1);
    }

    @Test
    void reintentaCuandoFallaLaRed() {
        String respuesta = Reintentos.ejecutar(() -> {
            if (intentos.incrementAndGet() < 3) {
                throw new ResourceAccessException("timeout");
            }
            return "ok";
        });

        assertThat(respuesta).isEqualTo("ok");
        assertThat(intentos).hasValue(Reintentos.INTENTOS);
    }

    @Test
    void despuesDelUltimoIntentoPropagaElError() {
        assertThatThrownBy(() -> Reintentos.ejecutar(() -> {
            intentos.incrementAndGet();
            throw new ResourceAccessException("sin red");
        })).isInstanceOf(ResourceAccessException.class);

        assertThat(intentos).hasValue(Reintentos.INTENTOS);
    }

    @Test
    void unaRespuestaDeErrorNoSeReintenta() {
        assertThatThrownBy(() -> Reintentos.ejecutar(() -> {
            intentos.incrementAndGet();
            throw new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR);
        })).isInstanceOf(HttpServerErrorException.class);

        assertThat(intentos).hasValue(1);
    }
}
