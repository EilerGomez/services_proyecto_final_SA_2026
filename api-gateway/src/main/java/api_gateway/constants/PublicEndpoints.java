package api_gateway.constants;

import java.util.Arrays;
import java.util.stream.Stream;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatcher;
import org.springframework.security.web.server.util.matcher.ServerWebExchangeMatchers;

/**
 * Catálogo de rutas que el gateway deja pasar sin exigir autenticación Bearer.
 * Cada grupo especifica también el método HTTP: que una consulta GET sea pública no hace
 * pública una operación POST sobre el mismo recurso. Se comparte un matcher entre la regla
 * de autorización y el convertidor Bearer para que ambas decisiones usen los mismos patrones.
 */
public final class PublicEndpoints {

    // Sesión y recuperación de identity segundo paso del doble factor.
    public static final String[] POST = {
        "/identity/auth/registro",
        "/identity/auth/login",
        "/identity/auth/login/verificar",
        "/identity/auth/refrescar",
        "/identity/auth/recuperar",
        "/identity/auth/restablecer",
        "/identity/auth/oauth2/canjear",
    };

    // Login con Google: URL de autorización, callback del proveedor y canje (canjear es POST).
    public static final String[] GET_OAUTH = {
        "/identity/auth/oauth2/**",
    };

    // Consultas públicas: Solo un segmento después del recurso
    public static final String[] GET_CONSULTAS = {
        "/fleet/sucursales", "/fleet/sucursales/*",
        "/trips/rutas", "/trips/rutas/*",
        "/trips/viajes", "/trips/viajes/*",
        "/sales/viajes/*/asientos",
    };

    public static final String[] GET_INFRAESTRUCTURA = Stream.concat(
            Stream.of(ApiPaths.HEALTH_ALL, ApiPaths.SWAGGER_HTML, ApiPaths.SWAGGER_UI,
                    ApiPaths.WEBJARS, ApiPaths.API_DOCS),
            Arrays.stream(ApiPaths.PREFIJOS).flatMap(prefijo -> Stream.of(
                    prefijo + ApiPaths.SWAGGER_HTML, prefijo + ApiPaths.SWAGGER_UI,
                    prefijo + ApiPaths.API_DOCS)))
            .toArray(String[]::new);

    // Evita crear instancias de esta clase utilitaria.
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
