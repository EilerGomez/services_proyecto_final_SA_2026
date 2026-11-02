package gt.cunoc.proyecto2.identity.integracion.soporte;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Azure Blob Storage de mentira: un servidor HTTP del JDK que responde lo que la prueba le indica y
 * guarda lo que recibió. Así la subida de fotos (I10) y el borrado de la foto reemplazada (I9) se
 * ejecutan de verdad contra el cliente REST, sin salir a Azure.
 *
 * <pre>
 * BLOBS.responderPrefijo("PUT", "/fotos/it/perfiles/", 201, "");   // el blob lleva un UUID en el nombre
 * BLOBS.responder("DELETE", "/fotos/it/perfiles/vieja.jpg", 202, "");
 * </pre>
 */
public final class BlobFalso {

    /** Petición recibida: método, ruta, query (ahí va el SAS), header Authorization y tamaño del cuerpo. */
    public record Peticion(String metodo, String ruta, String consulta, String autorizacion, int bytes,
                           String tipoContenido) {
    }

    private record Respuesta(int status, String cuerpo) {
    }

    private static final Respuesta SIN_CONFIGURAR = new Respuesta(404, "<Error><Code>BlobNotFound</Code></Error>");

    private final HttpServer servidor;
    private final Map<String, Respuesta> respuestas = new ConcurrentHashMap<>();
    private final Map<String, Respuesta> prefijos = new ConcurrentHashMap<>();
    private final List<Peticion> recibidas = new CopyOnWriteArrayList<>();

    private BlobFalso() {
        try {
            servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);   // puerto libre
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        servidor.createContext("/", this::atender);
        servidor.start();
    }

    public static BlobFalso iniciar() {
        return new BlobFalso();
    }

    /** URL base para app.fotos.cuenta-url, sin barra final. */
    public String url() {
        return "http://localhost:" + servidor.getAddress().getPort();
    }

    /** Lo que responde a "METODO ruta" exacta. */
    public void responder(String metodo, String ruta, int status, String cuerpo) {
        respuestas.put(metodo + " " + ruta, new Respuesta(status, cuerpo));
    }

    /** Lo que responde a cualquier ruta que empiece con ese prefijo (el blob lleva un UUID). */
    public void responderPrefijo(String metodo, String prefijoRuta, int status, String cuerpo) {
        prefijos.put(metodo + " " + prefijoRuta, new Respuesta(status, cuerpo));
    }

    public List<Peticion> peticiones() {
        return List.copyOf(recibidas);
    }

    /** Las peticiones que recibió con ese método, en orden. */
    public List<Peticion> peticiones(String metodo) {
        return recibidas.stream().filter(peticion -> peticion.metodo().equals(metodo)).toList();
    }

    /** Se llama antes de cada prueba. */
    public void reiniciar() {
        respuestas.clear();
        prefijos.clear();
        recibidas.clear();
    }

    private Optional<Respuesta> porPrefijo(String metodo, String ruta) {
        return prefijos.entrySet().stream()
                .filter(regla -> (metodo + " " + ruta).startsWith(regla.getKey()))
                .map(Map.Entry::getValue)
                .findFirst();
    }

    private void atender(HttpExchange intercambio) throws IOException {
        try (intercambio) {
            String metodo = intercambio.getRequestMethod();
            String ruta = intercambio.getRequestURI().getPath();
            byte[] cuerpo = intercambio.getRequestBody().readAllBytes();
            recibidas.add(new Peticion(metodo, ruta, intercambio.getRequestURI().getQuery(),
                    intercambio.getRequestHeaders().getFirst("Authorization"), cuerpo.length,
                    intercambio.getRequestHeaders().getFirst("x-ms-blob-content-type")));

            Respuesta respuesta = respuestas.get(metodo + " " + ruta);
            if (respuesta == null) {
                respuesta = porPrefijo(metodo, ruta).orElse(SIN_CONFIGURAR);
            }
            byte[] bytes = respuesta.cuerpo().getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().set("Content-Type", "application/xml");
            intercambio.sendResponseHeaders(respuesta.status(), bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream salida = intercambio.getResponseBody()) {
                    salida.write(bytes);
                }
            }
        }
    }
}
