package gt.cunoc.proyecto2.fleet.integracion.soporte;

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
 * Servidor HTTP mínimo (del JDK) que simula a otro microservicio (Identity o Trips) en las pruebas
 * de integración. Responde lo que la prueba le indica y guarda las peticiones que recibió.
 *
 * <pre>
 * IDENTITY.responder("POST", "/identity/interno/cuentas-personal", 201, "{...}");
 * ...
 * assertThat(IDENTITY.peticiones()).hasSize(1);
 * </pre>
 */
public final class ServidorFalso {

    /** Petición recibida: método, ruta, query, header Authorization y cuerpo. */
    public record Peticion(String metodo, String ruta, String consulta, String autorizacion, String cuerpo) {
    }

    private record Respuesta(int status, String cuerpo) {
    }

    private static final Respuesta SIN_CONFIGURAR =
            new Respuesta(404, "{\"status\":404,\"codigo\":\"RUTA_INEXISTENTE\",\"mensaje\":\"No configurada\"}");

    private final HttpServer servidor;
    private final Map<String, Respuesta> respuestas = new ConcurrentHashMap<>();
    private final Map<String, Respuesta> prefijos = new ConcurrentHashMap<>();
    private final List<Peticion> recibidas = new CopyOnWriteArrayList<>();

    private ServidorFalso() {
        try {
            servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);   // puerto libre
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        servidor.createContext("/", this::atender);
        servidor.start();
    }

    public static ServidorFalso iniciar() {
        return new ServidorFalso();
    }

    /** URL base para app.servicios.*-url. */
    public String url() {
        return "http://localhost:" + servidor.getAddress().getPort();
    }

    /** Lo que responde a "METODO ruta" (la ruta sin query). Un cuerpo vacío responde sin body. */
    public void responder(String metodo, String ruta, int status, String cuerpoJson) {
        respuestas.put(metodo + " " + ruta, new Respuesta(status, cuerpoJson));
    }

    /**
     * Lo que responde a cualquier ruta que empiece con ese prefijo. Sirve para las rutas que no se
     * pueden saber de antemano, como el blob de una foto, que lleva un UUID en el nombre.
     */
    public void responderPrefijo(String metodo, String prefijoRuta, int status, String cuerpoJson) {
        prefijos.put(metodo + " " + prefijoRuta, new Respuesta(status, cuerpoJson));
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
            String cuerpo = new String(intercambio.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            recibidas.add(new Peticion(metodo, ruta, intercambio.getRequestURI().getQuery(),
                    intercambio.getRequestHeaders().getFirst("Authorization"), cuerpo));

            Respuesta respuesta = respuestas.get(metodo + " " + ruta);
            if (respuesta == null) {
                respuesta = porPrefijo(metodo, ruta).orElse(SIN_CONFIGURAR);
            }
            byte[] bytes = respuesta.cuerpo().getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().set("Content-Type", "application/json");
            intercambio.sendResponseHeaders(respuesta.status(), bytes.length == 0 ? -1 : bytes.length);
            if (bytes.length > 0) {
                try (OutputStream salida = intercambio.getResponseBody()) {
                    salida.write(bytes);
                }
            }
        }
    }
}
