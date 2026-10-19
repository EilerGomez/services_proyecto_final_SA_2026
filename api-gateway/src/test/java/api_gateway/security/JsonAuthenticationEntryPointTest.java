package api_gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import api_gateway.exception.ErrorResponseWriter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.oauth2.jwt.JwtValidationException;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

class JsonAuthenticationEntryPointTest {

    private final JsonAuthenticationEntryPoint entrada =
            new JsonAuthenticationEntryPoint(new ErrorResponseWriter(JsonMapper.builder().build()));

    private static String cuerpo(MockServerWebExchange exchange) {
        return exchange.getResponse().getBodyAsString().block();
    }

    @Test
    void tokenRevocadoRespondeSesionRevocada() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/fleet/buses"));
        var revocado = new JwtValidationException("revocado",
                List.of(new OAuth2Error("SESION_REVOCADA", "Tu acceso cambió", null)));

        StepVerifier.create(entrada.commence(exchange, new InvalidBearerTokenException("revocado", revocado)))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode().value()).isEqualTo(401);
        assertThat(cuerpo(exchange)).contains("\"codigo\":\"SESION_REVOCADA\"");
    }

    @Test
    void otroTokenInvalidoSigueSiendoTokenInvalido() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/fleet/buses"));
        var vencido = new JwtValidationException("vencido", List.of(new OAuth2Error("invalid_token")));

        StepVerifier.create(entrada.commence(exchange, new InvalidBearerTokenException("vencido", vencido)))
                .verifyComplete();

        assertThat(cuerpo(exchange)).contains("\"codigo\":\"TOKEN_INVALIDO\"");
        assertThat(JsonAuthenticationEntryPoint.esSesionRevocada(null)).isFalse();
    }
}
