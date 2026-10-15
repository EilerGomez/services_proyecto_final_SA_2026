package gt.cunoc.proyecto2.fleet.clientes.almacenamiento;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Cliente mínimo de Azure Blob Storage (API REST "Put Blob" y "Delete Blob"), sin SDK.
 *
 * <p>Autenticación:
 * <ul>
 *   <li><b>En la VM de Azure</b> (sas vacío): token de la <i>identidad administrada</i> de la VM, pedido al
 *       endpoint de metadatos (IMDS). No hay claves en ningún archivo; la VM tiene el rol
 *       "Colaborador de datos de Storage Blob" solo sobre el contenedor.</li>
 *   <li><b>En local</b>: un token SAS del contenedor (variable FOTOS_SAS, solo en el .env local) con permiso
 *       de crear y escribir, y con fecha de vencimiento.</li>
 * </ul>
 * El token de la identidad se guarda en memoria y se renueva 5 minutos antes de vencer.
 */
public class ClienteBlobAzure {

    static final String VERSION_API = "2021-08-06";
    static final String CACHE_UN_ANIO = "public, max-age=31536000";
    static final String RECURSO_STORAGE = "https://storage.azure.com/";

    private static final Pattern TOKEN = Pattern.compile("\"access_token\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern VENCE = Pattern.compile("\"expires_on\"\\s*:\\s*\"?(\\d+)\"?");
    private static final Duration MARGEN = Duration.ofMinutes(5);
    private static final Duration ESPERA_SUBIDA = Duration.ofSeconds(20);
    private static final Duration ESPERA_IDENTIDAD = Duration.ofSeconds(5);

    private final String urlContenedor;
    private final String sas;
    private final String urlIdentidad;
    private final HttpClient http;
    private final Clock reloj;

    private String token;
    private Instant tokenVence = Instant.EPOCH;

    /**
     * @param cuentaUrl    https://&lt;cuenta&gt;.blob.core.windows.net
     * @param contenedor   nombre del contenedor (p. ej. "fotos")
     * @param sas          token SAS (con o sin "?" inicial); vacío para usar la identidad administrada
     * @param urlIdentidad endpoint de tokens de la identidad administrada (IMDS)
     */
    public ClienteBlobAzure(String cuentaUrl, String contenedor, String sas, String urlIdentidad,
                            HttpClient http, Clock reloj) {
        this.urlContenedor = sinBarraFinal(cuentaUrl) + "/" + contenedor + "/";
        this.sas = sas == null ? "" : sas.strip().replaceFirst("^\\?", "");
        this.urlIdentidad = urlIdentidad;
        this.http = http;
        this.reloj = reloj;
    }

    /** URL pública del contenedor, terminada en "/": https://&lt;cuenta&gt;.blob.core.windows.net/fotos/ */
    public String urlContenedor() {
        return urlContenedor;
    }

    /** Sube (o reemplaza) el blob. Lanza IOException si Azure no lo acepta o no responde. */
    public void subir(String objeto, byte[] contenido, String tipoMime) throws IOException {
        HttpRequest.Builder solicitud = HttpRequest.newBuilder(destino(objeto))
                .timeout(ESPERA_SUBIDA)
                .header("x-ms-version", VERSION_API)
                .header("x-ms-blob-type", "BlockBlob")
                .header("x-ms-blob-content-type", tipoMime)
                .header("x-ms-blob-cache-control", CACHE_UN_ANIO)
                .header("Content-Type", tipoMime)
                .PUT(HttpRequest.BodyPublishers.ofByteArray(contenido));
        HttpResponse<String> respuesta = enviar(autorizar(solicitud).build());
        if (respuesta.statusCode() != 201) {
            // El cuerpo es el XML de error de Azure (sin el SAS ni el token)
            throw new IOException("Azure Blob respondió " + respuesta.statusCode() + ": " + recortar(respuesta.body()));
        }
    }

    /** Borra el blob. Si ya no existe (404) no es error. Lanza IOException si Azure lo rechaza o no responde. */
    public void eliminar(String objeto) throws IOException {
        HttpRequest.Builder solicitud = HttpRequest.newBuilder(destino(objeto))
                .timeout(ESPERA_SUBIDA)
                .header("x-ms-version", VERSION_API)
                .DELETE();
        HttpResponse<String> respuesta = enviar(autorizar(solicitud).build());
        if (respuesta.statusCode() != 202 && respuesta.statusCode() != 404) {
            throw new IOException("Azure Blob respondió " + respuesta.statusCode() + " al borrar: "
                    + recortar(respuesta.body()));
        }
    }

    private URI destino(String objeto) {
        return URI.create(urlContenedor + objeto + (sas.isEmpty() ? "" : "?" + sas));
    }

    /** Con SAS la autorización va en la URL; sin SAS, el token de la identidad administrada. */
    private HttpRequest.Builder autorizar(HttpRequest.Builder solicitud) throws IOException {
        if (sas.isEmpty()) {
            solicitud.header("Authorization", "Bearer " + token());
        }
        return solicitud;
    }

    /** Token de la identidad administrada para Azure Storage (en memoria hasta 5 min antes de vencer). */
    synchronized String token() throws IOException {
        Instant ahora = reloj.instant();
        if (token != null && ahora.isBefore(tokenVence.minus(MARGEN))) {
            return token;
        }
        String url = urlIdentidad + "?api-version=2018-02-01&resource="
                + URLEncoder.encode(RECURSO_STORAGE, StandardCharsets.UTF_8);
        HttpResponse<String> respuesta = enviar(HttpRequest.newBuilder(URI.create(url))
                .timeout(ESPERA_IDENTIDAD)
                .header("Metadata", "true")
                .GET()
                .build());
        if (respuesta.statusCode() != 200) {
            throw new IOException("La identidad administrada respondió " + respuesta.statusCode());
        }
        Matcher valor = TOKEN.matcher(respuesta.body());
        if (!valor.find()) {
            throw new IOException("La identidad administrada no devolvió access_token");
        }
        Matcher vence = VENCE.matcher(respuesta.body());
        token = valor.group(1);
        tokenVence = vence.find() ? Instant.ofEpochSecond(Long.parseLong(vence.group(1))) : ahora.plus(MARGEN.multipliedBy(2));
        return token;
    }

    private HttpResponse<String> enviar(HttpRequest solicitud) throws IOException {
        try {
            return http.send(solicitud, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Llamada a Azure interrumpida", e);
        }
    }

    private static String sinBarraFinal(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static String recortar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.length() > 300 ? texto.substring(0, 300) + "..." : texto;
    }
}
