package api_gateway.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.netty.channel.ConnectTimeoutException;
import io.netty.handler.timeout.TimeoutException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.RequestPath;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.PrematureCloseException;
import reactor.test.StepVerifier;

class GlobalErrorWebExceptionHandlerTest {

    private ErrorResponseWriter writer;
    private GlobalErrorWebExceptionHandler handler;
    private ServerWebExchange exchange;
    private AtomicReference<HttpStatusCode> estadoEscrito;

    @BeforeEach
    void preparar() {
        writer = mock(ErrorResponseWriter.class);
        estadoEscrito = new AtomicReference<>();
        when(writer.write(any(), any())).thenAnswer(invocacion -> {
            estadoEscrito.set(invocacion.getArgument(1));
            return Mono.empty();
        });
        handler = new GlobalErrorWebExceptionHandler(writer);
        exchange = intercambio("/fleet/buses", false);
    }

    @Test
    void respuestaComprometidaDevuelveElErrorSinEscribir() {
        ServerWebExchange comprometido = intercambio("/fleet/buses", true);
        RuntimeException error = new RuntimeException("falla");

        StepVerifier.create(handler.handle(comprometido, error)).expectErrorMatches(error::equals).verify();

        verify(writer, never()).write(any(), any());
    }

    @Test
    void responseStatus4xxSePropaga() {
        StepVerifier.create(handler.handle(exchange, new ResponseStatusException(HttpStatus.NOT_FOUND)))
                .verifyComplete();

        assertThat(estadoEscrito.get()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(writer).write(any(), eq(HttpStatus.NOT_FOUND));
    }

    @Test
    void responseStatus5xxSePropaga() {
        StepVerifier.create(handler.handle(exchange, new ResponseStatusException(HttpStatus.BAD_GATEWAY)))
                .verifyComplete();

        assertThat(estadoEscrito.get()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    @Test
    void errorGenericoEs500() {
        StepVerifier.create(handler.handle(exchange, new RuntimeException("boom"))).verifyComplete();

        assertThat(estadoEscrito.get()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void causaAnidadaNoCoincidenteEs500() {
        StepVerifier.create(handler.handle(exchange,
                new RuntimeException("fuera", new IllegalStateException("dentro")))).verifyComplete();

        assertThat(estadoEscrito.get()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    void causaAnidadaConResponseStatusSePropaga() {
        StepVerifier.create(handler.handle(exchange,
                new RuntimeException("fuera", new ResponseStatusException(HttpStatus.FORBIDDEN))))
                .verifyComplete();

        assertThat(estadoEscrito.get()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void causaAnidadaConTimeoutEs504() {
        StepVerifier.create(handler.handle(exchange,
                new RuntimeException("fuera", new java.util.concurrent.TimeoutException("lento"))))
                .verifyComplete();

        assertThat(estadoEscrito.get()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }

    @Test
    void causaAnidadaConConexionEs503() {
        StepVerifier.create(handler.handle(exchange,
                new RuntimeException("fuera", new ConnectException("abajo")))).verifyComplete();

        assertThat(estadoEscrito.get()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void causaCiclicaNoSeCuelgaYEs500() {
        RuntimeException a = new RuntimeException("a");
        RuntimeException b = new RuntimeException("b", a);
        a.initCause(b);

        StepVerifier.create(handler.handle(exchange, a)).verifyComplete();

        assertThat(estadoEscrito.get()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ParameterizedTest(name = "timeout {0}")
    @ValueSource(classes = {
        java.util.concurrent.TimeoutException.class,
        TimeoutException.class,
        SocketTimeoutException.class,
        ConnectTimeoutException.class,
    })
    void tiemposDeEsperaSon504(Class<? extends Throwable> tipo) throws Exception {
        StepVerifier.create(handler.handle(exchange, instancia(tipo))).verifyComplete();

        assertThat(estadoEscrito.get()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
    }

    @ParameterizedTest(name = "no disponible {0}")
    @ValueSource(classes = {
        ConnectException.class,
        UnknownHostException.class,
        PrematureCloseException.class,
    })
    void fallosDeConexionSon503(Class<? extends Throwable> tipo) throws Exception {
        StepVerifier.create(handler.handle(exchange, instancia(tipo))).verifyComplete();

        assertThat(estadoEscrito.get()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    private static Throwable instancia(Class<? extends Throwable> tipo) throws Exception {
        if (tipo == PrematureCloseException.class || tipo == TimeoutException.class) {
            return mock(tipo);
        }
        try {
            return tipo.getConstructor(String.class).newInstance("falla");
        } catch (NoSuchMethodException e) {
            return tipo.getConstructor().newInstance();
        }
    }

    private static ServerWebExchange intercambio(String ruta, boolean comprometido) {
        ServerWebExchange exchange = mock(ServerWebExchange.class);
        ServerHttpRequest request = mock(ServerHttpRequest.class);
        ServerHttpResponse response = mock(ServerHttpResponse.class);
        when(exchange.getRequest()).thenReturn(request);
        when(exchange.getResponse()).thenReturn(response);
        RequestPath path = mock(RequestPath.class);
        when(path.value()).thenReturn(ruta);
        when(request.getPath()).thenReturn(path);
        when(response.isCommitted()).thenReturn(comprometido);
        return exchange;
    }
}
