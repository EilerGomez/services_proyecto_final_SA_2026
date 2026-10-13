package api_gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import api_gateway.constants.PublicEndpoints;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.authentication.BearerTokenAuthenticationToken;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import reactor.test.StepVerifier;

class RutasPublicasBearerTokenConverterTest {

    private static final String TOKEN_VENCIDO_O_INVALIDO = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ4In0.firma-invalida";

    private final RutasPublicasBearerTokenConverter converter =
            new RutasPublicasBearerTokenConverter(PublicEndpoints.matcher());

    @Test
    void rutaPublicaIgnoraElTokenYConservaElHeader() {
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/trips/viajes",
                "Bearer " + TOKEN_VENCIDO_O_INVALIDO);

        StepVerifier.create(converter.convert(exchange)).verifyComplete();

        assertThat(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                .isEqualTo("Bearer " + TOKEN_VENCIDO_O_INVALIDO);
    }

    @Test
    void rutaPublicaIgnoraUnHeaderMalFormado() {
        // En una ruta privada esto sería 401 ("Bearer token is malformed").
        MockServerWebExchange exchange = exchange(HttpMethod.POST, "/identity/auth/refrescar", "Bearer ###");

        StepVerifier.create(converter.convert(exchange)).verifyComplete();
    }

    @Test
    void rutaPublicaNoLlamaAlConverterDelegado() {
        ServerAuthenticationConverter delegado = mock(ServerAuthenticationConverter.class);
        RutasPublicasBearerTokenConverter conDelegado =
                new RutasPublicasBearerTokenConverter(PublicEndpoints.matcher(), delegado);

        StepVerifier.create(conDelegado.convert(exchange(HttpMethod.GET, "/fleet/sucursales",
                "Bearer " + TOKEN_VENCIDO_O_INVALIDO))).verifyComplete();

        verify(delegado, never()).convert(any());
    }

    @Test
    void mismoPathConOtroMetodoSiLeeElToken() {
        // GET /fleet/sucursales es público; POST /fleet/sucursales (F3) no.
        MockServerWebExchange exchange = exchange(HttpMethod.POST, "/fleet/sucursales",
                "Bearer " + TOKEN_VENCIDO_O_INVALIDO);

        StepVerifier.create(converter.convert(exchange))
                .assertNext(autenticacion -> assertThat(autenticacion)
                        .isInstanceOfSatisfying(BearerTokenAuthenticationToken.class,
                                token -> assertThat(token.getToken()).isEqualTo(TOKEN_VENCIDO_O_INVALIDO)))
                .verifyComplete();
    }

    @Test
    void rutaPrivadaLeeElToken() {
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/identity/cartera",
                "Bearer " + TOKEN_VENCIDO_O_INVALIDO);

        StepVerifier.create(converter.convert(exchange))
                .assertNext(autenticacion -> assertThat(autenticacion)
                        .isInstanceOf(BearerTokenAuthenticationToken.class))
                .verifyComplete();
    }

    @Test
    void rutaPrivadaSinTokenQuedaAnonima() {
        StepVerifier.create(converter.convert(exchange(HttpMethod.GET, "/identity/cartera", null)))
                .verifyComplete();
    }

    @Test
    void rutaPrivadaConHeaderMalFormadoFalla() {
        StepVerifier.create(converter.convert(exchange(HttpMethod.GET, "/identity/cartera", "Bearer ###")))
                .expectError(OAuth2AuthenticationException.class)
                .verify();
    }

    private static MockServerWebExchange exchange(HttpMethod metodo, String ruta, String autorizacion) {
        MockServerHttpRequest.BodyBuilder request = MockServerHttpRequest.method(metodo, URI.create(ruta));
        if (autorizacion != null) {
            request.header(HttpHeaders.AUTHORIZATION, autorizacion);
        }
        return MockServerWebExchange.from(request);
    }
}
