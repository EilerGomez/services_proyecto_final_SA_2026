package api_gateway.constants;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import reactor.test.StepVerifier;

class PublicEndpointsTest {

    private final ServerWebExchangeMatcher matcher = PublicEndpoints.matcher();

    @ParameterizedTest(name = "{0} {1} es pública")
    @CsvSource({
        // Identity (I1, I2, doble factor, I3, I5, I6)
        "POST, /identity/auth/registro",
        "POST, /identity/auth/login",
        "POST, /identity/auth/login/verificar",
        "POST, /identity/auth/refrescar",
        "POST, /identity/auth/recuperar",
        "POST, /identity/auth/restablecer",
        "POST, /identity/auth/oauth2/canjear",
        "GET, /identity/auth/oauth2/google/url",
        "GET, /identity/auth/oauth2/google/callback",
        // Fleet F1, F2
        "GET, /fleet/sucursales",
        "GET, /fleet/sucursales/7f3a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90",
        // Trips T3, T4, T8, T9
        "GET, /trips/rutas",
        "GET, /trips/rutas/7f3a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90",
        "GET, /trips/viajes",
        "GET, /trips/viajes/7f3a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90",
        // Sales S1
        "GET, /sales/viajes/7f3a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90/asientos",
        // Health y Swagger
        "GET, /actuator/health",
        "GET, /actuator/health/liveness",
        "GET, /swagger-ui.html",
        "GET, /webjars/swagger-ui/index.html",
        "GET, /v3/api-docs",
        "GET, /v3/api-docs/swagger-config",
        "GET, /v3/api-docs/fleet",
        "GET, /v3/api-docs/identity",
        "GET, /identity/swagger-ui.html",
        "GET, /identity/swagger-ui/index.html",
        "GET, /identity/v3/api-docs",
        "GET, /notifications/v3/api-docs/swagger-config",
        // Preflight CORS
        "OPTIONS, /sales/ventas",
    })
    void rutasPublicas(String metodo, String ruta) {
        assertThat(coincide(metodo, ruta)).isTrue();
    }

    @ParameterizedTest(name = "{0} {1} exige token")
    @CsvSource({
        // Mismo path, otro método
        "GET, /identity/auth/login",
        "POST, /fleet/sucursales",
        "PUT, /fleet/sucursales/7f3a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90",
        "PATCH, /fleet/sucursales/7f3a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90/estado",
        "POST, /trips/rutas",
        "DELETE, /trips/rutas/7f3a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90",
        "POST, /trips/viajes",
        "POST, /trips/viajes/7f3a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90/salida",
        "POST, /sales/viajes/7f3a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90/asientos",
        "POST, /v3/api-docs/fleet",
        // Rutas autenticadas
        "POST, /identity/auth/cerrar-sesion",
        "PUT, /identity/auth/contrasena",
        "PUT, /identity/auth/doble-factor",
        "GET, /identity/perfil",
        "GET, /identity/cartera",
        "GET, /fleet/sucursales/7f3a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90/personal",
        "GET, /fleet/buses",
        "GET, /trips/alquileres",
        "GET, /sales/ventas",
        "GET, /reports/ingresos",
        "GET, /notifications",
        "GET, /actuator/info",
        "GET, /actuator/gateway/routes",
        // Sin parecido parcial
        "POST, /identity/auth/login-falso",
        "GET, /fleet/sucursales-ocultas",
    })
    void rutasPrivadas(String metodo, String ruta) {
        assertThat(coincide(metodo, ruta)).isFalse();
    }

    private boolean coincide(String metodo, String ruta) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.method(HttpMethod.valueOf(metodo), URI.create(ruta)));
        boolean[] resultado = new boolean[1];
        StepVerifier.create(matcher.matches(exchange))
                .assertNext(match -> resultado[0] = match.isMatch())
                .verifyComplete();
        return resultado[0];
    }
}
