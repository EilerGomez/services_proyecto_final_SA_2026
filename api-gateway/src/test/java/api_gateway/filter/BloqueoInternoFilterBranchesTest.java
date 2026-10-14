package api_gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import api_gateway.exception.ErrorResponseWriter;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.PathContainer;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class BloqueoInternoFilterBranchesTest {

    private final BloqueoInternoFilter filtro = new BloqueoInternoFilter(
            new ErrorResponseWriter(tools.jackson.databind.json.JsonMapper.builder().build()));

    @Test
    void raizSoloTieneSeparadorYNoEsInterna() {
        assertThat(BloqueoInternoFilter.esRutaInterna(PathContainer.parsePath("/"))).isFalse();
    }

    @Test
    void puntoPuntoAlInicioSinSegmentosNoSeCuelga() {
        assertThat(BloqueoInternoFilter.esRutaInterna(PathContainer.parsePath("/../fleet/buses"))).isFalse();
        assertThat(BloqueoInternoFilter.esRutaInterna(PathContainer.parsePath("/../interno/x"))).isFalse();
    }

    @Test
    void puntoPuntoQueEliminaElPrefijoNoEsInterna() {
        assertThat(BloqueoInternoFilter.esRutaInterna(PathContainer.parsePath("/fleet/x/../../interno")))
                .isFalse();
        assertThat(BloqueoInternoFilter.esRutaInterna(PathContainer.parsePath("/a/b/../../interno/x")))
                .isFalse();
    }

    @Test
    void puntoCodificadoSeIgnora() {
        assertThat(BloqueoInternoFilter.esRutaInterna(PathContainer.parsePath("/fleet/%2e/interno")))
                .isTrue();
    }

    @Test
    void barraCodificadaDentroDelSegmentoSeSepara() {
        assertThat(BloqueoInternoFilter.esRutaInterna(PathContainer.parsePath("/fleet/interno%2F/buses")))
                .isTrue();
    }

    @Test
    void segundoSegmentoDistintoNoEsInterna() {
        assertThat(BloqueoInternoFilter.esRutaInterna(PathContainer.parsePath("/fleet/buses/interno")))
                .isFalse();
    }

    @ParameterizedTest(name = "deja pasar {0}")
    @ValueSource(strings = {
        "/",
        "/../fleet/buses",
        "/fleet/x/../../interno",
        "/fleet/././buses",
    })
    void dejaPasarRutasLimite(String ruta) {
        AtomicBoolean siguio = new AtomicBoolean(false);
        MockServerWebExchange exchange =
                MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.GET, URI.create(ruta)));
        WebFilterChain chain = posterior -> {
            siguio.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filtro.filter(exchange, chain)).verifyComplete();

        assertThat(siguio).isTrue();
    }

    @Test
    void puntoPuntoNormalizadoAInternoSiBloquea() {
        AtomicBoolean siguio = new AtomicBoolean(false);
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.GET, URI.create("/../fleet/interno/x")));
        WebFilterChain chain = posterior -> {
            siguio.set(true);
            return Mono.empty();
        };

        StepVerifier.create(filtro.filter(exchange, chain)).verifyComplete();

        assertThat(siguio).isFalse();
    }
}
