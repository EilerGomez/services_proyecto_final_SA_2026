package gt.cunoc.proyecto2.identity.integracion.auth;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.identity.integracion.soporte.PruebaIntegracion;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * OAuth2 con Google configurado apuntando a un Google falso (sin red externa):
 * flujo completo crear/vincular/canjear más los errores del proveedor.
 */
class OAuthGoogleIT extends PruebaIntegracion {

    private static final String CLIENTE_ID = "it-client";
    private static final GoogleFalso GOOGLE = GoogleFalso.iniciar(CLIENTE_ID);

    @DynamicPropertySource
    static void oauthFalso(DynamicPropertyRegistry registro) {
        registro.add("app.oauth2.google.client-id", () -> CLIENTE_ID);
        registro.add("app.oauth2.google.client-secret", () -> "it-secret");
        registro.add("app.oauth2.google.token-url", () -> GOOGLE.urlBase() + "/token");
        registro.add("app.oauth2.google.jwks-url", () -> GOOGLE.urlBase() + "/certs");
    }

    @Value("${local.server.port}")
    private int puerto;

    private final HttpClient sinRedirect = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

    @Test
    void urlDevuelveAutorizacionConState() {
        ResponseEntity<String> respuesta = get("/identity/auth/oauth2/google/url", null);
        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.url"))
                .contains("accounts.google.com", CLIENTE_ID);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.state")).isNotBlank();
    }

    @Test
    void flujoCompletoCreaClienteYCanjeaSesion() {
        String ticket = ticketDe("ok");
        ResponseEntity<String> canje = post("/identity/auth/oauth2/canjear", null,
                "{\"ticket\":\"%s\"}".formatted(ticket));
        assertThat(canje.getStatusCode().value()).as(canje.getBody()).isEqualTo(200);
        assertThat(PruebaIntegracion.<String>json(canje, "$.usuario.roles[0]")).isEqualTo("CLIENTE");

        UUID usuarioId = UUID.fromString(PruebaIntegracion.<String>json(canje, "$.usuarioId"));
        assertThat(proveedorDe(usuarioId)).isEqualTo("GOOGLE");
        // La cartera la abre wallet-service al recibir CuentaCreada con rol CLIENTE
        Map<String, String> eventos = esperarEventos("CuentaCreada");
        assertThat(eventos.get("CuentaCreada")).contains(GoogleFalso.CORREO).contains("CLIENTE");
    }

    @Test
    void vinculaCuentaLocalExistenteSinDuplicarNiElevar() {
        UUID local = registrarCliente(GoogleFalso.CORREO, "2000000-1", "2000000000101");
        String ticket = ticketDe("ok");
        ResponseEntity<String> canje = post("/identity/auth/oauth2/canjear", null,
                "{\"ticket\":\"%s\"}".formatted(ticket));
        assertThat(canje.getStatusCode().value()).as(canje.getBody()).isEqualTo(200);
        assertThat(PruebaIntegracion.<String>json(canje, "$.usuarioId")).isEqualTo(local.toString());
        assertThat(proveedorDe(local)).isEqualTo("GOOGLE");
        assertThat(contar("usuarios")).isEqualTo(2); // admin espía + el vinculado
    }

    @Test
    void correoNoVerificadoRedirigeConError() throws Exception {
        String estado = estadoDeUrl();
        HttpResponse<Void> respuesta = sinRedirect.send(
                HttpRequest.newBuilder(URI.create(callback("sin-verificar", estado))).GET().build(),
                HttpResponse.BodyHandlers.discarding());
        assertThat(respuesta.statusCode()).isEqualTo(302);
        assertThat(respuesta.headers().firstValue("location").orElse(""))
                .contains("OAUTH_CORREO_NO_VERIFICADO");
    }

    @Test
    void googleCaidoRedirigeConError() throws Exception {
        String estado = estadoDeUrl();
        HttpResponse<Void> respuesta = sinRedirect.send(
                HttpRequest.newBuilder(URI.create(callback("mal", estado))).GET().build(),
                HttpResponse.BodyHandlers.discarding());
        assertThat(respuesta.statusCode()).isEqualTo(302);
        assertThat(respuesta.headers().firstValue("location").orElse(""))
                .contains("GOOGLE_SIN_RESPUESTA");
    }

    @Test
    void ticketReusadoDa401() {
        String ticket = ticketDe("ok");
        assertThat(post("/identity/auth/oauth2/canjear", null,
                "{\"ticket\":\"%s\"}".formatted(ticket)).getStatusCode().value()).isEqualTo(200);
        esError(post("/identity/auth/oauth2/canjear", null,
                "{\"ticket\":\"%s\"}".formatted(ticket)), 401, "OAUTH_TICKET_INVALIDO");
    }

    // ---------- apoyos ----------

    private String estadoDeUrl() {
        ResponseEntity<String> url = get("/identity/auth/oauth2/google/url", null);
        assertThat(url.getStatusCode().value()).isEqualTo(200);
        return PruebaIntegracion.json(url, "$.state");
    }

    private String callback(String codigo, String estado) {
        return "http://localhost:" + puerto + "/identity/auth/oauth2/google/callback?code="
                + codigo + "&state=" + estado;
    }

    /** Completa url + callback y devuelve el ticket de la redirección. */
    private String ticketDe(String codigo) {
        try {
            String estado = estadoDeUrl();
            HttpResponse<Void> respuesta = sinRedirect.send(
                    HttpRequest.newBuilder(URI.create(callback(codigo, estado))).GET().build(),
                    HttpResponse.BodyHandlers.discarding());
            assertThat(respuesta.statusCode()).isEqualTo(302);
            String destino = respuesta.headers().firstValue("location").orElse("");
            assertThat(destino).as("callback con code=" + codigo).contains("?ticket=");
            return destino.substring(destino.indexOf("ticket=") + 7);
        } catch (Exception e) {
            throw new IllegalStateException("Callback OAuth falló con code=" + codigo, e);
        }
    }

    private String proveedorDe(UUID usuarioId) {
        return jdbc.queryForObject(
                "SELECT proveedor FROM usuarios WHERE id = ?", String.class, usuarioId);
    }
}
