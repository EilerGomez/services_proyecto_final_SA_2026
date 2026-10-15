package api_gateway.constants;

import java.util.Arrays;
import java.util.stream.Stream;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

/**
 * Rutas que pasan el gateway sin token (columna "Público" de 01..07_*.md).
 * Cada ruta va con su método: por ejemplo GET /fleet/sucursales es público, pero
 * POST /fleet/sucursales (F3) exige token. Los patrones se comparan con la ruta ya
 * decodificada y sin parámetros de matriz (PathPattern de Spring).
 */
public final class PublicEndpoints {

    /** Sesión y recuperación de identity: I1, I2, segundo paso del doble factor, I3, I5 e I6. */
    public static final String[] POST = {
        "/identity/auth/registro",
        "/identity/auth/login",
        "/identity/auth/login/verificar",
        "/identity/auth/refrescar",
        "/identity/auth/recuperar",
        "/identity/auth/restablecer",
        "/identity/auth/oauth2/canjear",
    };

    /** Login con Google: URL de autorización, callback del proveedor y canje (canjear es POST). */
    public static final String[] GET_OAUTH = {
        "/identity/auth/oauth2/**",
    };

    /**
     * Consultas públicas: F1–F2, T3–T4, T8–T9 y S1. Solo un segmento después del recurso
     * ("/*"), así GET /fleet/sucursales/{id}/personal (F6) sigue exigiendo token.
     * En T8/T9 el header Authorization se reenvía: si viene token, el servicio decide por rol.
     */
    public static final String[] GET_CONSULTAS = {
        "/fleet/sucursales", "/fleet/sucursales/*",
        "/trips/rutas", "/trips/rutas/*",
        "/trips/viajes", "/trips/viajes/*",
        "/sales/viajes/*/asientos",
    };

    /**
     * Health del gateway, Swagger agregado del gateway (swagger-ui, webjars y /v3/api-docs/**,
     * que incluye /v3/api-docs/{servicio} y swagger-config) y el Swagger de cada servicio
     * por su prefijo (/identity/swagger-ui.html, /identity/v3/api-docs, ...).
     */
    public static final String[] GET_INFRAESTRUCTURA = Stream.concat(
            Stream.of(ApiPaths.HEALTH_ALL, ApiPaths.SWAGGER_HTML, ApiPaths.SWAGGER_UI,
                    ApiPaths.WEBJARS, ApiPaths.API_DOCS),
            Arrays.stream(ApiPaths.PREFIJOS).flatMap(prefijo -> Stream.of(
                    prefijo + ApiPaths.SWAGGER_HTML, prefijo + ApiPaths.SWAGGER_UI,
                    prefijo + ApiPaths.API_DOCS)))
            .toArray(String[]::new);

    private PublicEndpoints() { }

    /**
     * Matcher único de rutas públicas: lo usan la autorización (permitAll) y el lector del
     * Bearer token (que ignora el token en estas rutas). Incluye los preflight OPTIONS.
     */
    public static ServerWebExchangeMatcher matcher() {
        return ServerWebExchangeMatchers.matchers(
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.OPTIONS, "/**"),
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.POST, POST),
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, GET_OAUTH),
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, GET_CONSULTAS),
                ServerWebExchangeMatchers.pathMatchers(HttpMethod.GET, GET_INFRAESTRUCTURA));
    }
}
