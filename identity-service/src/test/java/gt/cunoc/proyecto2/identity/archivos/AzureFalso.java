package gt.cunoc.proyecto2.identity.archivos;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Servidor HTTP del JDK que simula, en las pruebas, al endpoint de identidad administrada (IMDS)
 * en "/token" y a Azure Blob Storage en cualquier otra ruta. Sin red ni credenciales reales.
 */
public final class AzureFalso implements AutoCloseable {

    /** Petición recibida (cabeceras en minúsculas). */
    public record Peticion(String metodo, String ruta, String consulta, Map<String, String> cabeceras, byte[] cuerpo) {
        public String cabecera(String nombre) {
            return cabeceras.get(nombre.toLowerCase());
        }
    }

    private final HttpServer servidor;
    private final List<Peticion> recibidas = new CopyOnWriteArrayList<>();
    private volatile int estadoToken = 200;
    private volatile String cuerpoToken = "{\"access_token\":\"tok-1\",\"expires_on\":\"" + Long.MAX_VALUE / 2000 + "\"}";
    private volatile int estadoSubida = 201;
    private volatile String cuerpoSubida = "";
    private volatile int estadoBorrado = 202;

    private AzureFalso() {
        try {
            servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        servidor.createContext("/", this::atender);
        servidor.start();
    }

    public static AzureFalso iniciar() {
        return new AzureFalso();
    }

    /** URL base: hace de cuenta-url ("http://localhost:puerto") y de IMDS (url() + "/token"). */
    public String url() {
        return "http://localhost:" + servidor.getAddress().getPort();
    }

    public void responderToken(int estado, String cuerpo) {
        this.estadoToken = estado;
        this.cuerpoToken = cuerpo;
    }

    public void responderSubida(int estado, String cuerpo) {
        this.estadoSubida = estado;
        this.cuerpoSubida = cuerpo;
    }

    public void responderBorrado(int estado) {
        this.estadoBorrado = estado;
    }

    public List<Peticion> peticiones() {
        return List.copyOf(recibidas);
    }

    public List<Peticion> pedidosDeToken() {
        return recibidas.stream().filter(p -> p.ruta().equals("/token")).toList();
    }

    public List<Peticion> subidas() {
        return recibidas.stream().filter(p -> p.metodo().equals("PUT")).toList();
    }

    public List<Peticion> borrados() {
        return recibidas.stream().filter(p -> p.metodo().equals("DELETE")).toList();
    }

    @Override
    public void close() {
        servidor.stop(0);
    }

    private void atender(HttpExchange intercambio) throws IOException {
        try (intercambio) {
            Map<String, String> cabeceras = new java.util.HashMap<>();
            intercambio.getRequestHeaders().forEach((nombre, valores) -> cabeceras.put(nombre.toLowerCase(), valores.get(0)));
            String ruta = intercambio.getRequestURI().getPath();
            recibidas.add(new Peticion(intercambio.getRequestMethod(), ruta, intercambio.getRequestURI().getRawQuery(),
                    cabeceras, intercambio.getRequestBody().readAllBytes()));
            boolean esToken = ruta.equals("/token");
            boolean esBorrado = intercambio.getRequestMethod().equals("DELETE");
            byte[] cuerpo = (esToken ? cuerpoToken : esBorrado ? "" : cuerpoSubida).getBytes(StandardCharsets.UTF_8);
            int estado = esToken ? estadoToken : esBorrado ? estadoBorrado : estadoSubida;
            intercambio.sendResponseHeaders(estado, cuerpo.length == 0 ? -1 : cuerpo.length);
            if (cuerpo.length > 0) {
                try (OutputStream salida = intercambio.getResponseBody()) {
                    salida.write(cuerpo);
                }
            }
        }
    }
}
