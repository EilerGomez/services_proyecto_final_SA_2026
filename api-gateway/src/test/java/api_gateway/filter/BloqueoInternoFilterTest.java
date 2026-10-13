package api_gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import api_gateway.exception.ErrorResponseWriter;
import java.net.URI;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

class BloqueoInternoFilterTest {

    private final BloqueoInternoFilter filtro =
            new BloqueoInternoFilter(new ErrorResponseWriter(JsonMapper.builder().build()));

    @ParameterizedTest(name = "bloquea {0}")
    @ValueSource(strings = {
        "/fleet/interno/buses/1/disponibilidad",
        "/fleet/interno/choferes/1",
        "/trips/interno/viajes/1/vendible",
        "/identity/interno/cuentas-personal",
        "/fleet/interno",
        "/fleet/interno/",
        // Parámetros de matriz
        "/fleet/interno;x=1/buses/1/disponibilidad",
        "/fleet/interno;jsessionid=abc/buses",
        // Codificación por porcentaje
        "/fleet/%69nterno/buses/1/disponibilidad",
        "/fleet/%69%6E%74%65%72%6E%6F/buses",
        "/fleet/%49NTERNO/buses",
        // Mayúsculas
        "/fleet/INTERNO/buses",
        "/fleet/Interno/buses",
        "/FLEET/interno/buses",
        // Rutas sin normalizar que el servidor del servicio resolvería a /fleet/interno/...
        "/fleet//interno/buses",
        "/fleet/./interno/buses",
        "/fleet/x/../interno/buses",
        "/fleet/x/%2e%2e/interno/buses",
        "/fleet%2Finterno/buses",
    })
    void bloqueaRutasInternasCon404(String ruta) {
        AtomicBoolean siguio = new AtomicBoolean(false);
        MockServerWebExchange exchange = exchange(HttpMethod.GET, ruta);

        StepVerifier.create(filtro.filter(exchange, cadena(siguio))).verifyComplete();

        assertThat(siguio).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        StepVerifier.create(exchange.getResponse().getBodyAsString())
                .assertNext(cuerpo -> assertThat(cuerpo)
                        .contains("\"status\":404")
                        .contains("\"codigo\":\"RUTA_INEXISTENTE\""))
                .verifyComplete();
    }

    @Test
    void bloqueaTambienOtrosMetodos() {
        AtomicBoolean siguio = new AtomicBoolean(false);
        MockServerWebExchange exchange = exchange(HttpMethod.POST, "/identity/interno/cuentas-personal");

        StepVerifier.create(filtro.filter(exchange, cadena(siguio))).verifyComplete();

        assertThat(siguio).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @ParameterizedTest(name = "deja pasar {0}")
    @ValueSource(strings = {
        "/fleet/buses",
        "/fleet/buses/1",
        "/fleet/internos-algo",
        "/fleet/internos",
        "/fleet/interno-x/buses",
        "/fleet/xinterno/buses",
        "/fleet/buses/interno",
        "/interno",
        "/interno/buses",
        "/fleet/interno/../buses",
        "/fleet/sucursales",
        "/v3/api-docs/fleet",
        "/actuator/health",
        "/",
    })
    void dejaPasarRutasQueNoSonInternas(String ruta) {
        AtomicBoolean siguio = new AtomicBoolean(false);
        MockServerWebExchange exchange = exchange(HttpMethod.GET, ruta);

        StepVerifier.create(filtro.filter(exchange, cadena(siguio))).verifyComplete();

        assertThat(siguio).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNotEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void esRutaInternaUsaSegmentosDecodificados() {
        assertThat(BloqueoInternoFilter.esRutaInterna(PathContainer.parsePath("/fleet/%69nterno;a=b/x"))).isTrue();
        assertThat(BloqueoInternoFilter.esRutaInterna(PathContainer.parsePath("/fleet/internos-algo"))).isFalse();
        assertThat(BloqueoInternoFilter.esRutaInterna(PathContainer.parsePath(""))).isFalse();
    }

    private static MockServerWebExchange exchange(HttpMethod metodo, String ruta) {
        // URI.create no vuelve a codificar: la ruta llega cruda, como la mandaría un cliente.
        return MockServerWebExchange.from(MockServerHttpRequest.method(metodo, URI.create(ruta)));
    }

    private static WebFilterChain cadena(AtomicBoolean siguio) {
        return exchange -> {
            siguio.set(true);
            return Mono.empty();
        };
    }
}
