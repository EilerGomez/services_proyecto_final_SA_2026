package api_gateway.revocaciones;

import static org.assertj.core.api.Assertions.assertThat;

import api_gateway.support.JwtPrueba;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

/** Identity falso: la primera vez no responde bien y la segunda devuelve las revocaciones. */
class CargaInicialRevocacionesTest {

    private final RegistroRevocaciones registro = new RegistroRevocaciones(Duration.ofMinutes(65));
    private final TokenServicio token = new TokenServicio(JwtPrueba.SECRETO, JsonMapper.builder().build(),
            Clock.systemUTC());
    private DisposableServer identity;

    @AfterEach
    void apagar() {
        if (identity != null) {
            identity.disposeNow();
        }
    }

    @Test
    void cargaLasRevocacionesRecientesConTokenDeServicioYReintenta() {
        Instant desde = Instant.now().minusSeconds(30);
        AtomicInteger llamadas = new AtomicInteger();
        AtomicReference<String> autorizacion = new AtomicReference<>();
        identity = HttpServer.create().host("127.0.0.1").port(0)
                .route(rutas -> rutas.get(CargaInicialRevocaciones.RUTA, (peticion, respuesta) -> {
                    autorizacion.set(peticion.requestHeaders().get("Authorization"));
                    if (llamadas.incrementAndGet() == 1) {
                        return respuesta.status(503).send();
                    }
                    return respuesta.header("Content-Type", "application/json").sendString(Mono.just("""
                            [{"usuarioId":"ana","tokensValidosDesde":"%s"},{"usuarioId":"incompleta"}]
                            """.formatted(desde)));
                }))
                .bindNow();
        CargaInicialRevocaciones carga = new CargaInicialRevocaciones(
                WebClient.create("http://127.0.0.1:" + identity.port()), token, registro, 3, Duration.ofMillis(50));

        StepVerifier.create(carga.cargar()).expectNext(1).verifyComplete();

        assertThat(llamadas).hasValue(2);
        assertThat(autorizacion.get()).startsWith("Bearer ");
        assertThat(registro.revocado("ana", desde.minusSeconds(1))).isTrue();
    }

    @Test
    void siIdentityNoRespondeSeRindeSinTumbarElGateway() {
        CargaInicialRevocaciones carga = new CargaInicialRevocaciones(
                WebClient.create("http://127.0.0.1:1"), token, registro, 1, Duration.ofMillis(10));

        carga.alArrancar();     // solo registra el error en el log
        StepVerifier.create(carga.cargar()).expectError().verify(Duration.ofSeconds(10));
        assertThat(registro.cantidad()).isZero();
    }
}
