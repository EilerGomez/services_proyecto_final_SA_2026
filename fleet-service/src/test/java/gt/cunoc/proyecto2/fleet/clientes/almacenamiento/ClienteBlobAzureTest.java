package gt.cunoc.proyecto2.fleet.clientes.almacenamiento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Cliente de Azure Blob contra un Azure simulado (IMDS + Put Blob) con un reloj controlado. */
class ClienteBlobAzureTest {

    private static final byte[] FOTO = "contenido-de-la-foto".getBytes(StandardCharsets.UTF_8);

    private AzureFalso azure;
    private Reloj reloj;

    /** Reloj que la prueba puede adelantar (para el vencimiento del token). */
    static final class Reloj extends Clock {
        private Instant ahora = Instant.parse("2026-10-20T15:00:00Z");

        void adelantar(long segundos) {
            ahora = ahora.plusSeconds(segundos);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    @BeforeEach
    void preparar() {
        azure = AzureFalso.iniciar();
        reloj = new Reloj();
    }

    @AfterEach
    void cerrar() {
        azure.close();
    }

    private ClienteBlobAzure cliente(String sas) {
        return new ClienteBlobAzure(azure.url() + "/", "fotos", sas, azure.url() + "/token",
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(), reloj);
    }

    @Test
    void laUrlDelContenedorTerminaEnBarra() {
        assertThat(cliente("").urlContenedor()).isEqualTo(azure.url() + "/fotos/");
        assertThat(new ClienteBlobAzure("https://cuenta.blob.core.windows.net", "fotos", null, "x",
                HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build(), reloj).urlContenedor())
                .isEqualTo("https://cuenta.blob.core.windows.net/fotos/");
    }

    @Test
    void subeConElTokenDeLaIdentidadAdministrada() throws IOException {
        cliente("").subir("dev/perfiles/a.jpg", FOTO, "image/jpeg");

        AzureFalso.Peticion token = azure.pedidosDeToken().get(0);
        assertThat(token.metodo()).isEqualTo("GET");
        assertThat(token.cabecera("Metadata")).isEqualTo("true");
        assertThat(token.consulta()).contains("api-version=2018-02-01").contains("resource=https%3A%2F%2Fstorage.azure.com%2F");

        AzureFalso.Peticion subida = azure.subidas().get(0);
        assertThat(subida.metodo()).isEqualTo("PUT");
        assertThat(subida.ruta()).isEqualTo("/fotos/dev/perfiles/a.jpg");
        assertThat(subida.consulta()).isNull();
        assertThat(subida.cabecera("Authorization")).isEqualTo("Bearer tok-1");
        assertThat(subida.cabecera("x-ms-blob-type")).isEqualTo("BlockBlob");
        assertThat(subida.cabecera("x-ms-version")).isEqualTo(ClienteBlobAzure.VERSION_API);
        assertThat(subida.cabecera("x-ms-blob-content-type")).isEqualTo("image/jpeg");
        assertThat(subida.cabecera("x-ms-blob-cache-control")).isEqualTo(ClienteBlobAzure.CACHE_UN_ANIO);
        assertThat(subida.cuerpo()).isEqualTo(FOTO);
    }

    @Test
    void reutilizaElTokenHastaCincoMinutosAntesDeVencer() throws IOException {
        long vence = reloj.instant().plusSeconds(3600).getEpochSecond();
        azure.responderToken(200, "{\"token_type\":\"Bearer\",\"access_token\":\"tok-a\",\"expires_on\":\"" + vence + "\"}");
        ClienteBlobAzure cliente = cliente("");

        cliente.subir("a.png", FOTO, "image/png");
        cliente.subir("b.png", FOTO, "image/png");
        assertThat(azure.pedidosDeToken()).hasSize(1);

        reloj.adelantar(3600 - 299);                      // a menos de 5 minutos de vencer
        azure.responderToken(200, "{\"access_token\":\"tok-b\",\"expires_on\":" + (vence + 3600) + "}");
        cliente.subir("c.png", FOTO, "image/png");

        assertThat(azure.pedidosDeToken()).hasSize(2);
        assertThat(azure.subidas().get(2).cabecera("Authorization")).isEqualTo("Bearer tok-b");
    }

    @Test
    void sinFechaDeVencimientoElTokenDuraDiezMinutos() throws IOException {
        azure.responderToken(200, "{\"access_token\":\"tok-sin-fecha\"}");
        ClienteBlobAzure cliente = cliente("");

        assertThat(cliente.token()).isEqualTo("tok-sin-fecha");
        reloj.adelantar(4 * 60);
        cliente.token();
        assertThat(azure.pedidosDeToken()).hasSize(1);
        reloj.adelantar(2 * 60);
        cliente.token();
        assertThat(azure.pedidosDeToken()).hasSize(2);
    }

    @Test
    void conSasNoPideTokenYLoAgregaALaUrl() throws IOException {
        cliente("?sv=2022-11-02&sp=cw&sig=abc").subir("dev/buses/x.webp", FOTO, "image/webp");

        assertThat(azure.pedidosDeToken()).isEmpty();
        AzureFalso.Peticion subida = azure.subidas().get(0);
        assertThat(subida.consulta()).isEqualTo("sv=2022-11-02&sp=cw&sig=abc");
        assertThat(subida.cabecera("Authorization")).isNull();
    }

    @Test
    void borraElBlobConElTokenDeLaIdentidad() throws IOException {
        cliente("").eliminar("dev/perfiles/vieja.jpg");

        AzureFalso.Peticion borrado = azure.borrados().get(0);
        assertThat(borrado.ruta()).isEqualTo("/fotos/dev/perfiles/vieja.jpg");
        assertThat(borrado.cabecera("Authorization")).isEqualTo("Bearer tok-1");
        assertThat(borrado.cabecera("x-ms-version")).isEqualTo(ClienteBlobAzure.VERSION_API);
    }

    @Test
    void borrarConSasYBorrarAlgoQueYaNoExisteNoEsError() throws IOException {
        azure.responderBorrado(404);

        cliente("sig=abc").eliminar("dev/buses/no-existe.png");

        assertThat(azure.borrados().get(0).consulta()).isEqualTo("sig=abc");
        assertThat(azure.pedidosDeToken()).isEmpty();
    }

    @Test
    void siAzureRechazaElBorradoLanzaIOException() {
        azure.responderBorrado(403);

        assertThatThrownBy(() -> cliente("").eliminar("a.jpg"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("403")
                .hasMessageContaining("al borrar");
    }

    @Test
    void siAzureRechazaLaSubidaLanzaIOExceptionConElCodigo() {
        azure.responderSubida(403, "<Error><Code>AuthorizationPermissionMismatch</Code></Error>");

        assertThatThrownBy(() -> cliente("").subir("a.jpg", FOTO, "image/jpeg"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("403")
                .hasMessageContaining("AuthorizationPermissionMismatch");
    }

    @Test
    void unErrorLargoDeAzureSeRecorta() {
        azure.responderSubida(500, "x".repeat(1000));

        assertThatThrownBy(() -> cliente("").subir("a.jpg", FOTO, "image/jpeg"))
                .isInstanceOf(IOException.class)
                .hasMessageEndingWith("...");
    }

    @Test
    void sinIdentidadAdministradaNoSube() {
        azure.responderToken(400, "{\"error\":\"invalid_request\"}");

        assertThatThrownBy(() -> cliente("").subir("a.jpg", FOTO, "image/jpeg"))
                .isInstanceOf(IOException.class)
                .hasMessageContaining("400");
        assertThat(azure.subidas()).isEmpty();
    }

    @Test
    void unaRespuestaDeIdentidadSinTokenEsUnError() {
        azure.responderToken(200, "{\"expires_on\":\"123\"}");

        assertThatThrownBy(() -> cliente("").token())
                .isInstanceOf(IOException.class)
                .hasMessageContaining("access_token");
    }

    @Test
    @SuppressWarnings("unchecked")
    void unaRespuestaSinCuerpoNoRompeElMensaje() throws Exception {
        HttpClient http = mock(HttpClient.class);
        HttpResponse<String> respuesta = mock(HttpResponse.class);
        when(respuesta.statusCode()).thenReturn(503);
        when(respuesta.body()).thenReturn(null);
        when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenReturn((HttpResponse) respuesta);
        ClienteBlobAzure cliente = new ClienteBlobAzure("http://cuenta", "fotos", "sig=1", "http://imds", http, reloj);

        assertThatThrownBy(() -> cliente.subir("a.jpg", FOTO, "image/jpeg"))
                .isInstanceOf(IOException.class)
                .hasMessage("Azure Blob respondió 503: ");
    }

    @Test
    @SuppressWarnings("unchecked")
    void siSeInterrumpeLaLlamadaLanzaIOExceptionYConservaLaInterrupcion() throws Exception {
        HttpClient http = mock(HttpClient.class);
        when(http.send(any(), any(HttpResponse.BodyHandler.class))).thenThrow(new InterruptedException("corte"));
        ClienteBlobAzure cliente = new ClienteBlobAzure("http://cuenta", "fotos", "", "http://imds", http, reloj);

        try {
            assertThatThrownBy(cliente::token).isInstanceOf(IOException.class).hasMessageContaining("interrumpida");
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            Thread.interrupted();                          // limpia la marca para las demás pruebas
        }
    }
}
