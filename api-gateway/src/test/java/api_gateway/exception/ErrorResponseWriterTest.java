package api_gateway.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import api_gateway.constants.HeaderNames;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

class ErrorResponseWriterTest {

    private final ErrorResponseWriter writer = new ErrorResponseWriter(JsonMapper.builder().build());

    @ParameterizedTest(name = "{0} -> {1} / {2}")
    @CsvSource({
        "401, TOKEN_INVALIDO, No autenticado",
        "403, PROHIBIDO, Acceso denegado",
        "404, RUTA_INEXISTENTE, No encontrado",
        "500, ERROR_INTERNO, Error interno",
        "503, SERVICIO_NO_DISPONIBLE, Servicio no disponible",
        "504, TIEMPO_AGOTADO, Tiempo de espera agotado",
    })
    void escribeCodigoYMensajeSegunEstado(int estado, String codigo, String mensaje) {
        MockServerWebExchange exchange = exchange("/fleet/buses");
        HttpStatus status = HttpStatus.valueOf(estado);

        StepVerifier.create(writer.write(exchange, status)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(status);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(exchange.getResponse().getHeaders().getContentLength()).isGreaterThan(0);
        StepVerifier.create(exchange.getResponse().getBodyAsString())
                .assertNext(cuerpo -> {
                    assertThat(cuerpo).contains("\"status\":" + estado);
                    assertThat(cuerpo).contains("\"codigo\":\"" + codigo + "\"");
                    assertThat(cuerpo).contains("\"mensaje\":\"" + mensaje + "\"");
                    assertThat(cuerpo).contains("\"ruta\":\"/fleet/buses\"");
                })
                .verifyComplete();
    }

    @Test
    void estadoDesconocidoUsaCodigoGenerico() {
        MockServerWebExchange exchange = exchange("/trips/viajes");

        StepVerifier.create(writer.write(exchange, HttpStatus.I_AM_A_TEAPOT)).verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.I_AM_A_TEAPOT);
        StepVerifier.create(exchange.getResponse().getBodyAsString())
                .assertNext(cuerpo -> {
                    assertThat(cuerpo).contains("\"codigo\":\"HTTP_418\"");
                    assertThat(cuerpo).contains("\"mensaje\":\"La solicitud falló\"");
                })
                .verifyComplete();
    }

    @Test
    void estado400TambienUsaCodigoGenerico() {
        MockServerWebExchange exchange = exchange("/sales/ventas");

        StepVerifier.create(writer.write(exchange, HttpStatus.BAD_REQUEST)).verifyComplete();

        StepVerifier.create(exchange.getResponse().getBodyAsString())
                .assertNext(cuerpo -> {
                    assertThat(cuerpo).contains("\"codigo\":\"HTTP_400\"");
                    assertThat(cuerpo).contains("\"mensaje\":\"La solicitud falló\"");
                })
                .verifyComplete();
    }

    @Test
    void conservaElCorrelationIdEnviado() {
        MockServerHttpRequest.BodyBuilder request = MockServerHttpRequest.method(HttpMethod.GET, URI.create("/fleet/buses"))
                .header(HeaderNames.CORRELATION_ID, "corr-123");
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        StepVerifier.create(writer.write(exchange, HttpStatus.NOT_FOUND)).verifyComplete();

        assertThat(exchange.getResponse().getHeaders().getFirst(HeaderNames.CORRELATION_ID))
                .isEqualTo("corr-123");
        StepVerifier.create(exchange.getResponse().getBodyAsString())
                .assertNext(cuerpo -> assertThat(cuerpo).contains("\"correlationId\":\"corr-123\""))
                .verifyComplete();
    }

    @Test
    void generaCorrelationIdCuandoFalta() {
        MockServerWebExchange exchange = exchange("/fleet/buses");

        StepVerifier.create(writer.write(exchange, HttpStatus.INTERNAL_SERVER_ERROR)).verifyComplete();

        String correlacion = exchange.getResponse().getHeaders().getFirst(HeaderNames.CORRELATION_ID);
        assertThat(correlacion).isNotBlank();
        String finalCorrelacion = correlacion;
        StepVerifier.create(exchange.getResponse().getBodyAsString())
                .assertNext(cuerpo -> assertThat(cuerpo).contains("\"correlationId\":\"" + finalCorrelacion + "\""))
                .verifyComplete();
    }

    @Test
    void respuestaComprometidaNoEscribe() {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        when(exchange.getResponse()).thenReturn(response);
        when(response.isCommitted()).thenReturn(true);

        StepVerifier.create(writer.write(exchange, HttpStatus.NOT_FOUND)).verifyComplete();

        verify(response, never()).writeWith(any());
        verify(response, never()).setStatusCode(any());
    }

    private static MockServerWebExchange exchange(String ruta) {
        return MockServerWebExchange.from(MockServerHttpRequest.method(HttpMethod.GET, URI.create(ruta)));
    }

    @Test
    void escribeUnCodigoYMensajePropios() {
        MockServerWebExchange exchange = exchange("/wallet/cartera");

        StepVerifier.create(writer.write(exchange, HttpStatus.UNAUTHORIZED, "SESION_REVOCADA", "Tu acceso cambió"))
                .verifyComplete();

        StepVerifier.create(exchange.getResponse().getBodyAsString())
                .assertNext(cuerpo -> {
                    assertThat(cuerpo).contains("\"codigo\":\"SESION_REVOCADA\"");
                    assertThat(cuerpo).contains("\"mensaje\":\"Tu acceso cambió\"");
                })
                .verifyComplete();
    }
}
