package api_gateway.filter;

import static org.assertj.core.api.Assertions.assertThat;

import api_gateway.constants.HeaderNames;
import java.net.URI;
import java.security.Principal;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest.BodyBuilder;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class UserContextFilterTest {

    private final UserContextFilter filtro = new UserContextFilter();

    @Test
    void sinPrincipalSoloLimpiaFalsificados() {
        BodyBuilder peticion = base()
                .header(HeaderNames.USER_ID, "falso")
                .header(HeaderNames.USER_ROLES, "ADMIN_SISTEMA")
                .header(HeaderNames.USER_EMAIL, "falso@x.com")
                .header(HeaderNames.SUCURSAL_ID, "falsa")
                .header(HeaderNames.CHOFER_ID, "falso");

        HttpHeaders enviados = filtrar(peticion, Mono.empty());

        assertThat(enviados.getFirst(HeaderNames.USER_ID)).isNull();
        assertThat(enviados.getFirst(HeaderNames.USER_ROLES)).isNull();
        assertThat(enviados.getFirst(HeaderNames.USER_EMAIL)).isNull();
        assertThat(enviados.getFirst(HeaderNames.SUCURSAL_ID)).isNull();
        assertThat(enviados.getFirst(HeaderNames.CHOFER_ID)).isNull();
    }

    @Test
    void principalNoJwtSoloLimpia() {
        BodyBuilder peticion = base().header(HeaderNames.USER_ID, "falso");
        Principal basico = new UsernamePasswordAuthenticationToken("usuario", "clave");

        HttpHeaders enviados = filtrar(peticion, Mono.just(basico));

        assertThat(enviados.getFirst(HeaderNames.USER_ID)).isNull();
        assertThat(enviados.getFirst(HeaderNames.USER_ROLES)).isNull();
    }

    @Test
    void jwtNoAutenticadoSoloLimpia() {
        JwtAuthenticationToken token = token(claimsCompletos());
        token.setAuthenticated(false);
        BodyBuilder peticion = base().header(HeaderNames.USER_ID, "falso");

        HttpHeaders enviados = filtrar(peticion, Mono.just((Principal) token));

        assertThat(enviados.getFirst(HeaderNames.USER_ID)).isNull();
        assertThat(enviados.getFirst(HeaderNames.USER_ROLES)).isNull();
    }

    @Test
    void jwtValidoPropagaTodoElContexto() {
        JwtAuthenticationToken token = token(claimsCompletos());

        HttpHeaders enviados = filtrar(base(), Mono.just((Principal) token));

        assertThat(enviados.getFirst(HeaderNames.USER_ID)).isEqualTo("usuario-1");
        assertThat(enviados.getFirst(HeaderNames.USER_EMAIL)).isEqualTo("usuario@correo.com");
        assertThat(enviados.getFirst(HeaderNames.SUCURSAL_ID)).isEqualTo("suc-1");
        assertThat(enviados.getFirst(HeaderNames.CHOFER_ID)).isEqualTo("cho-1");
        assertThat(enviados.getFirst(HeaderNames.USER_ROLES)).isEqualTo("ADMIN_SISTEMA,CAJERO");
    }

    @Test
    void reemplazaHeadersFalsificadosConLosDelToken() {
        BodyBuilder peticion = base()
                .header(HeaderNames.USER_ID, "falso")
                .header(HeaderNames.USER_ROLES, "FALSO");

        HttpHeaders enviados = filtrar(peticion, Mono.just((Principal) token(claimsCompletos())));

        assertThat(enviados.getFirst(HeaderNames.USER_ID)).isEqualTo("usuario-1");
        assertThat(enviados.getFirst(HeaderNames.USER_ROLES)).isEqualTo("ADMIN_SISTEMA,CAJERO");
    }

    @Test
    void claimsNulosNoGeneranHeaders() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "usuario-2");
        claims.put("roles", null);

        HttpHeaders enviados = filtrar(base(), Mono.just((Principal) token(claims)));

        assertThat(enviados.getFirst(HeaderNames.USER_ID)).isEqualTo("usuario-2");
        assertThat(enviados.getFirst(HeaderNames.USER_EMAIL)).isNull();
        assertThat(enviados.getFirst(HeaderNames.SUCURSAL_ID)).isNull();
        assertThat(enviados.getFirst(HeaderNames.CHOFER_ID)).isNull();
        assertThat(enviados.getFirst(HeaderNames.USER_ROLES)).isNull();
    }

    @Test
    void claimsEnBlancoSeOmiten() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "   ");
        claims.put("correo", "  ");
        claims.put("sucursalId", "");
        claims.put("roles", List.of());

        HttpHeaders enviados = filtrar(base(), Mono.just((Principal) token(claims)));

        assertThat(enviados.getFirst(HeaderNames.USER_ID)).isNull();
        assertThat(enviados.getFirst(HeaderNames.USER_EMAIL)).isNull();
        assertThat(enviados.getFirst(HeaderNames.SUCURSAL_ID)).isNull();
        assertThat(enviados.getFirst(HeaderNames.USER_ROLES)).isNull();
    }

    @Test
    void rolesMixtosSoloConservanStringsConTexto() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "usuario-3");
        claims.put("roles", Arrays.asList("CLIENTE", "", "  ", 123, null, "ADMIN_SISTEMA", "CLIENTE"));

        HttpHeaders enviados = filtrar(base(), Mono.just((Principal) token(claims)));

        assertThat(enviados.getFirst(HeaderNames.USER_ROLES)).isEqualTo("CLIENTE,ADMIN_SISTEMA");
    }

    @Test
    void rolesNoColeccionSeIgnoran() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "usuario-4");
        claims.put("roles", "ADMIN_SISTEMA");

        HttpHeaders enviados = filtrar(base(), Mono.just((Principal) token(claims)));

        assertThat(enviados.getFirst(HeaderNames.USER_ROLES)).isNull();
    }

    @Test
    void claimNumericoSeConvierteATexto() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "usuario-5");
        claims.put("correo", 42);
        claims.put("roles", List.of("CLIENTE"));

        HttpHeaders enviados = filtrar(base(), Mono.just((Principal) token(claims)));

        assertThat(enviados.getFirst(HeaderNames.USER_EMAIL)).isEqualTo("42");
    }

    @Test
    void ordenEsAltaPrecedenciaMasDos() {
        assertThat(filtro.getOrder()).isEqualTo(Ordered.HIGHEST_PRECEDENCE + 2);
    }

    private HttpHeaders filtrar(BodyBuilder peticion, Mono<Principal> principal) {
        ServerWebExchange exchange = MockServerWebExchange.from(peticion).mutate().principal(principal).build();
        AtomicReference<ServerWebExchange> capturado = new AtomicReference<>();
        GatewayFilterChain chain = posterior -> {
            capturado.set(posterior);
            return Mono.empty();
        };

        StepVerifier.create(filtro.filter(exchange, chain)).verifyComplete();

        assertThat(capturado.get()).isNotNull();
        return capturado.get().getRequest().getHeaders();
    }

    private static BodyBuilder base() {
        return MockServerHttpRequest.method(HttpMethod.GET, URI.create("/fleet/buses"));
    }

    private static JwtAuthenticationToken token(Map<String, Object> claims) {
        Instant ahora = Instant.now();
        Jwt jwt = new Jwt("token", ahora, ahora.plusSeconds(3600), Map.of("alg", "HS256"), claims);
        return new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_CLIENTE")));
    }

    private static Map<String, Object> claimsCompletos() {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", "usuario-1");
        claims.put("correo", "usuario@correo.com");
        claims.put("sucursalId", "suc-1");
        claims.put("choferId", "cho-1");
        claims.put("roles", List.of("ADMIN_SISTEMA", "CAJERO"));
        return claims;
    }
}
