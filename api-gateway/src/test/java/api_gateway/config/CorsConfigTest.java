package api_gateway.config;

import static org.assertj.core.api.Assertions.assertThat;

import api_gateway.constants.HeaderNames;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.DefaultCorsProcessor;

class CorsConfigTest {

    private static final String FRONTEND = "http://localhost:5173";
    /** URL absoluta: CorsUtils compara esquema, host y puerto con el Origin. */
    private static final String GATEWAY = "http://localhost:8080";

    private static final String ANGULAR = "http://localhost:4200";

    @Test
    void segundoOrigenPermitido() {
        CorsConfigurationSource multi = fuente(FRONTEND, ANGULAR);

        boolean angular = new DefaultCorsProcessor().process(
                multi.getCorsConfiguration(preflight(ANGULAR)), preflight(ANGULAR));
        boolean vite = new DefaultCorsProcessor().process(
                multi.getCorsConfiguration(preflight(FRONTEND)), preflight(FRONTEND));
        boolean otro = new DefaultCorsProcessor().process(
                multi.getCorsConfiguration(preflight("http://otro.example")), preflight("http://otro.example"));

        assertThat(angular).isTrue();
        assertThat(vite).isTrue();
        assertThat(otro).isFalse();
    }

    private static MockServerWebExchange preflight(String origen) {
        return MockServerWebExchange.from(MockServerHttpRequest.options(GATEWAY + "/identity/auth/login")
                .header(HttpHeaders.ORIGIN, origen)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"));
    }

    private static CorsConfigurationSource fuente(String... origenes) {
        GatewayProperties propiedades = new GatewayProperties();
        propiedades.setAllowedOrigins(List.of(origenes));
        return new CorsConfig().corsConfigurationSource(propiedades);
    }

    private final CorsConfigurationSource source = fuente(FRONTEND);

    @Test
    void exponeCorrelationIdYContentDisposition() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(GATEWAY + "/reports/ingresos/pdf").header(HttpHeaders.ORIGIN, FRONTEND));

        CorsConfiguration cors = source.getCorsConfiguration(exchange);

        assertThat(cors).isNotNull();
        assertThat(cors.getExposedHeaders())
                .contains(HeaderNames.CORRELATION_ID, HttpHeaders.CONTENT_DISPOSITION);
        assertThat(cors.getAllowedOrigins()).containsExactly(FRONTEND);
        assertThat(cors.getAllowCredentials()).isTrue();
    }

    @Test
    void laRespuestaCorsIncluyeContentDispositionEnExposeHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(GATEWAY + "/reports/ingresos/pdf").header(HttpHeaders.ORIGIN, FRONTEND));

        boolean permitido = new DefaultCorsProcessor().process(source.getCorsConfiguration(exchange), exchange);

        assertThat(permitido).isTrue();
        HttpHeaders respuesta = exchange.getResponse().getHeaders();
        assertThat(respuesta.getAccessControlAllowOrigin()).isEqualTo(FRONTEND);
        assertThat(respuesta.getAccessControlExposeHeaders())
                .contains(HeaderNames.CORRELATION_ID, HttpHeaders.CONTENT_DISPOSITION);
    }

    @Test
    void origenNoPermitidoSeRechaza() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(GATEWAY + "/fleet/sucursales")
                        .header(HttpHeaders.ORIGIN, "http://otro.example"));

        boolean permitido = new DefaultCorsProcessor().process(source.getCorsConfiguration(exchange), exchange);

        assertThat(permitido).isFalse();
    }

    private static CorsConfigurationSource fuente() {
        GatewayProperties propiedades = new GatewayProperties();
        propiedades.setAllowedOrigins(List.of(FRONTEND));
        return new CorsConfig().corsConfigurationSource(propiedades);
    }
}
