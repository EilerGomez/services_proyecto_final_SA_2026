package gt.cunoc.proyecto2.identity.integracion.auth;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.identity.integracion.soporte.PruebaIntegracion;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

/**
 * OAuth2 con Google sin proveedor real: sin credenciales el servicio responde
 * 503/302/401 controlados y el resto sigue igual (las credenciales nunca van al repo).
 */
class OAuthIT extends PruebaIntegracion {

    @Value("${local.server.port}")
    private int puerto;

    @Test
    void urlSinConfigDa503() {
        esError(get("/identity/auth/oauth2/google/url", null), 503, "OAUTH_NO_CONFIGURADO");
    }

    @Test
    void canjearTicketMaloDa401() {
        esError(post("/identity/auth/oauth2/canjear", null, "{\"ticket\":\"inexistente\"}"),
                401, "OAUTH_TICKET_INVALIDO");
    }

    @Test
    void callbackSinConfigPriorizaErrorDeConfiguracion() throws Exception {
        // Sin configurar, el error de configuración precede a la validación del state.
        HttpClient sinRedirect = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        HttpResponse<Void> respuesta = sinRedirect.send(
                HttpRequest.newBuilder(URI.create(
                        "http://localhost:" + puerto + "/identity/auth/oauth2/google/callback?state=zzz"))
                        .GET().build(),
                HttpResponse.BodyHandlers.discarding());
        assertThat(respuesta.statusCode()).isEqualTo(302);
        assertThat(respuesta.headers().firstValue("location").orElse(""))
                .contains("OAUTH_NO_CONFIGURADO");
    }

    @Test
    void callbackSinConfigRedirigeConError() throws Exception {
        HttpClient sinRedirect = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        HttpResponse<Void> respuesta = sinRedirect.send(
                HttpRequest.newBuilder(URI.create(
                        "http://localhost:" + puerto
                                + "/identity/auth/oauth2/google/callback?code=x&state=y"))
                        .GET().build(),
                HttpResponse.BodyHandlers.discarding());
        assertThat(respuesta.statusCode()).isEqualTo(302);
        assertThat(respuesta.headers().firstValue("location").orElse(""))
                .contains("OAUTH_NO_CONFIGURADO");
    }
}
