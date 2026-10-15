package gt.cunoc.proyecto2.identity.services.oauth;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Cliente HTTP hacia Google sin red (servidor simulado). */
class ClienteHttpGoogleRestTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer servidor = MockRestServiceServer.createServer(builder);
    private final ClienteHttpGoogleRest cliente = new ClienteHttpGoogleRest(builder);

    @Test
    void postFormularioDevuelveElMapa() {
        servidor.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andRespond(withSuccess("{\"id_token\":\"abc\"}", MediaType.APPLICATION_JSON));

        Map<String, Object> respuesta = cliente.postFormulario("https://oauth2.googleapis.com/token",
                Map.of("code", "c"));

        assertEquals("abc", respuesta.get("id_token"));
        servidor.verify();
    }

    @Test
    void postSinCuerpoYGetSinCuerpoDanErrorPropio() {
        servidor.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        servidor.expect(requestTo("https://www.googleapis.com/oauth2/v3/certs"))
                .andRespond(withSuccess("", MediaType.APPLICATION_JSON));
        assertThrows(IllegalStateException.class,
                () -> cliente.postFormulario("https://oauth2.googleapis.com/token", Map.of()));
        assertThrows(IllegalStateException.class,
                () -> cliente.getTexto("https://www.googleapis.com/oauth2/v3/certs"));
        servidor.verify();
    }

    @Test
    void errorDeGoogleSePropaga() {
        servidor.expect(requestTo("https://oauth2.googleapis.com/token"))
                .andRespond(withServerError());
        assertThrows(RuntimeException.class,
                () -> cliente.postFormulario("https://oauth2.googleapis.com/token", Map.of()));
        servidor.verify();
    }

    @Test
    void getTextoDevuelveElCuerpo() {
        servidor.expect(requestTo("https://www.googleapis.com/oauth2/v3/certs"))
                .andRespond(withSuccess("{\"keys\":[]}", MediaType.APPLICATION_JSON));
        assertEquals("{\"keys\":[]}", cliente.getTexto("https://www.googleapis.com/oauth2/v3/certs"));
        servidor.verify();
    }
}
