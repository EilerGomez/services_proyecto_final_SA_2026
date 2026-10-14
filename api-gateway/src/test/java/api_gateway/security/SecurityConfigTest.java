package api_gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import api_gateway.support.JwtPrueba;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

/**
 * Gateway completo (Netty en puerto aleatorio) con la seguridad activada. Los 7 servicios se
 * reemplazan por un servidor HTTP local que responde 200 y devuelve en headers lo que recibió
 * (ruta, Authorization y X-User-*), para comprobar qué deja pasar el gateway y qué reenvía.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "gateway.security.enabled=true",
    "app.jwt.secret=" + JwtPrueba.SECRETO,
    "gateway.allowed-origins=" + SecurityConfigTest.ORIGEN,
})
class SecurityConfigTest {

    static final String ORIGEN = "http://localhost:5173";
    private static final String UN_ID = "7f3a9c1e-5b7d-4e8a-9c21-7d4e5f6a8b90";
    /** Lo que devuelve el servicio falso cuando no recibió el header. */
    private static final String SIN_HEADER = "(sin header)";

    private static DisposableServer servicio;

    @Value("${local.server.port}")
    private int puerto;

    private WebTestClient cliente;

    @DynamicPropertySource
    static void servicioFalso(DynamicPropertyRegistry registro) {
        servicio = HttpServer.create().host("127.0.0.1").port(0)
                .handle((request, response) -> response.status(200)
                        .header("Content-Type", "application/json")
                        .header("X-Recibido-Ruta", request.uri())
                        .header("X-Recibido-Authorization", valor(request.requestHeaders().get("Authorization")))
                        .header("X-Recibido-User-Id", valor(request.requestHeaders().get("X-User-Id")))
                        .header("X-Recibido-User-Roles", valor(request.requestHeaders().get("X-User-Roles")))
                        .sendString(Mono.just("{\"ok\":true}")))
                .bindNow();
        String url = "http://127.0.0.1:" + servicio.port();
        for (String nombre : List.of("identity", "fleet", "trips", "sales", "maintenance", "reporting",
                "notification")) {
            registro.add("gateway.services." + nombre + "-url", () -> url);
        }
    }

    @AfterAll
    static void detenerServicioFalso() {
        if (servicio != null) {
            servicio.disposeNow();
        }
    }

    @BeforeEach
    void crearCliente() {
        cliente = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + puerto)
                .responseTimeout(Duration.ofSeconds(15))
                .build();
    }

    // ---------- Rutas públicas ----------

    @Test
    void rutaPublicaSinTokenLlegaAlServicio() {
        cliente.get().uri("/fleet/sucursales").exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Recibido-Ruta", "/fleet/sucursales");
    }

    @Test
    void endpointsPublicosDeIdentitySinToken() {
        for (String ruta : List.of("/identity/auth/registro", "/identity/auth/login",
                "/identity/auth/login/verificar", "/identity/auth/refrescar",
                "/identity/auth/recuperar", "/identity/auth/restablecer")) {
            cliente.post().uri(ruta).contentType(MediaType.APPLICATION_JSON).bodyValue("{}").exchange()
                    .expectStatus().isOk()
                    .expectHeader().valueEquals("X-Recibido-Ruta", ruta);
        }
    }

    @Test
    void consultasPublicasSinToken() {
        for (String ruta : List.of("/fleet/sucursales/" + UN_ID, "/trips/rutas", "/trips/rutas/" + UN_ID,
                "/trips/viajes", "/trips/viajes/" + UN_ID, "/sales/viajes/" + UN_ID + "/asientos",
                "/identity/auth/oauth2/google/url", "/v3/api-docs/fleet", "/identity/v3/api-docs")) {
            cliente.get().uri(ruta).exchange().expectStatus().isOk();
        }
    }

    @Test
    void rutaPublicaConTokenVencidoNoEs401YReenviaElHeader() {
        String vencido = JwtPrueba.vencido(JwtPrueba.SECRETO, UUID.randomUUID(), "CAJERO");

        cliente.get().uri("/trips/viajes").header(HttpHeaders.AUTHORIZATION, "Bearer " + vencido).exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Recibido-Authorization", "Bearer " + vencido);
    }

    @Test
    void rutaPublicaConTokenInvalidoNoEs401() {
        cliente.get().uri("/sales/viajes/" + UN_ID + "/asientos")
                .header(HttpHeaders.AUTHORIZATION, "Bearer esto.no.sirve").exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Recibido-Authorization", "Bearer esto.no.sirve");
    }

    @Test
    void rutaPublicaConTokenMalFormadoNoEs401() {
        cliente.post().uri("/identity/auth/refrescar").header(HttpHeaders.AUTHORIZATION, "Bearer ###")
                .contentType(MediaType.APPLICATION_JSON).bodyValue("{}").exchange()
                .expectStatus().isOk();
    }

    @Test
    void healthEsPublico() {
        cliente.get().uri("/actuator/health").exchange()
                .expectStatus().isOk()
                .expectBody().jsonPath("$.status").isEqualTo("UP");
    }

    @Test
    void preflightSinTokenEsPermitido() {
        cliente.options().uri("/sales/ventas")
                .header(HttpHeaders.ORIGIN, ORIGEN)
                .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN);
    }

    // ---------- Rutas privadas ----------

    @Test
    void rutaPrivadaSinTokenEs401() {
        cliente.get().uri("/identity/cartera").exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().valueEquals(HttpHeaders.WWW_AUTHENTICATE, "Bearer")
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.codigo").isEqualTo("TOKEN_INVALIDO");
    }

    @Test
    void mismoPathConOtroMetodoExigeToken() {
        cliente.post().uri("/fleet/sucursales").contentType(MediaType.APPLICATION_JSON).bodyValue("{}")
                .exchange()
                .expectStatus().isUnauthorized();
        cliente.post().uri("/sales/viajes/" + UN_ID + "/asientos").exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void subrutaNoPublicaExigeToken() {
        cliente.get().uri("/fleet/sucursales/" + UN_ID + "/personal").exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void rutaPrivadaConTokenVencidoEs401() {
        String vencido = JwtPrueba.vencido(JwtPrueba.SECRETO, UUID.randomUUID(), "CLIENTE");

        cliente.get().uri("/identity/cartera").header(HttpHeaders.AUTHORIZATION, "Bearer " + vencido).exchange()
                .expectStatus().isUnauthorized()
                .expectBody().jsonPath("$.codigo").isEqualTo("TOKEN_INVALIDO");
    }

    @Test
    void rutaPrivadaConTokenDeOtroSecretoEs401() {
        String ajeno = JwtPrueba.vigente("otro-secreto-distinto-con-mas-de-32-caracteres!!", UUID.randomUUID(),
                null, "CLIENTE");

        cliente.get().uri("/identity/cartera").header(HttpHeaders.AUTHORIZATION, "Bearer " + ajeno).exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void rutaPrivadaConTokenValidoPasaConElContextoDelUsuario() {
        UUID usuario = UUID.randomUUID();
        String token = JwtPrueba.vigente(JwtPrueba.SECRETO, usuario, null, "CLIENTE");

        cliente.get().uri("/identity/cartera").header(HttpHeaders.AUTHORIZATION, "Bearer " + token).exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Recibido-Ruta", "/identity/cartera")
                .expectHeader().valueEquals("X-Recibido-Authorization", "Bearer " + token)
                .expectHeader().valueEquals("X-Recibido-User-Id", usuario.toString())
                .expectHeader().valueEquals("X-Recibido-User-Roles", "CLIENTE");
    }

    @Test
    void elClienteNoPuedeFalsificarXUserId() {
        cliente.get().uri("/fleet/sucursales").header("X-User-Id", UN_ID).exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals("X-Recibido-User-Id", SIN_HEADER);
    }

    // ---------- Rutas internas, actuator y CORS ----------

    @Test
    void rutasInternasDan404AunqueTenganTokenValido() {
        String token = JwtPrueba.vigente(JwtPrueba.SECRETO, UUID.randomUUID(), null, "SERVICIO");
        for (String ruta : List.of("/fleet/interno/buses/" + UN_ID + "/disponibilidad",
                "/fleet/%69nterno/buses/" + UN_ID + "/disponibilidad",
                "/fleet/interno;x=1/buses/" + UN_ID + "/disponibilidad",
                "/trips/INTERNO/viajes/" + UN_ID + "/vendible")) {
            cliente.get().uri(URI.create("http://localhost:" + puerto + ruta))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).exchange()
                    .expectStatus().isNotFound()
                    .expectBody().jsonPath("$.codigo").isEqualTo("RUTA_INEXISTENTE");
        }
    }

    @Test
    void actuatorGatewayNoSeExpone() {
        String token = JwtPrueba.vigente(JwtPrueba.SECRETO, UUID.randomUUID(), null, "ADMIN_SISTEMA");

        cliente.get().uri("/actuator/gateway/routes").header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void corsExponeContentDispositionYCorrelationId() {
        cliente.get().uri("/fleet/sucursales").header(HttpHeaders.ORIGIN, ORIGEN).exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEN)
                .expectHeader().values(HttpHeaders.ACCESS_CONTROL_EXPOSE_HEADERS, valores -> assertThat(
                        String.join(",", valores)).contains(HttpHeaders.CONTENT_DISPOSITION, "X-Correlation-Id"));
    }

    @Test
    void statusDeLasRutasPublicasNoEs401() {
        cliente.get().uri("/trips/rutas/" + UN_ID).header(HttpHeaders.AUTHORIZATION, "Bearer a.b.c").exchange()
                .expectStatus().value(status -> assertThat(status).isNotEqualTo(HttpStatus.UNAUTHORIZED.value()));
    }

    private static String valor(String header) {
        return header == null ? SIN_HEADER : header;
    }
}
