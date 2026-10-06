/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gt.cunoc.proyecto2.comun.cliente;

/**
 *
 * @author eiler
 */
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import gt.cunoc.proyecto2.comun.eventos.contextocorrelacion.ContextoCorrelacion;
import gt.cunoc.proyecto2.comun.seguridad.service.ServicioJwt;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/** Usa un servidor HTTP real del JDK (puerto libre) para ver las cabeceras que llegan. */
class ClientesInternosTest {

    private static final String SECRETO = "secreto-de-prueba-con-mas-de-32-caracteres";

    private final ServicioJwt jwt =
            new ServicioJwt(JsonMapper.builder().build(), SECRETO, Duration.ofMinutes(60), Clock.systemUTC());
    private final ClientesInternos clientes = new ClientesInternos(jwt, "fleet-service");

    private final AtomicReference<String> autorizacion = new AtomicReference<>();
    private final AtomicReference<String> correlacion = new AtomicReference<>();
    private HttpServer servidor;
    private String urlBase;

    @BeforeEach
    void levantarServidor() throws IOException {
        servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        servidor.createContext("/trips/interno/viajes/activos", intercambio -> {
            autorizacion.set(intercambio.getRequestHeaders().getFirst("Authorization"));
            correlacion.set(intercambio.getRequestHeaders().getFirst(ContextoCorrelacion.HEADER));
            byte[] cuerpo = "[]".getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(200, cuerpo.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(cuerpo);
            }
        });
        servidor.createContext("/lento", intercambio -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            intercambio.sendResponseHeaders(200, -1);
            intercambio.close();
        });
        servidor.start();
        urlBase = "http://127.0.0.1:" + servidor.getAddress().getPort();
    }

    @AfterEach
    void detenerServidor() {
        servidor.stop(0);
        ContextoCorrelacion.limpiar();
    }

    @Test
    void enviaTokenDeServicioValido() {
        String respuesta = clientes.crear(urlBase).get()
                .uri("/trips/interno/viajes/activos?busId={id}", UUID.randomUUID())
                .retrieve().body(String.class);

        assertThat(respuesta).isEqualTo("[]");
        assertThat(autorizacion.get()).startsWith("Bearer ");
        UsuarioActual quien = jwt.validar(autorizacion.get().substring("Bearer ".length()));
        assertThat(quien.esServicio()).isTrue();
        assertThat(quien.correo()).isEqualTo("fleet-service");
        assertThat(quien.usuarioId()).isEqualTo(ServicioJwt.idServicio("fleet-service"));
    }

    @Test
    void propagaElIdDeCorrelacionActual() {
        UUID actual = UUID.randomUUID();
        ContextoCorrelacion.establecer(actual);

        clientes.crear(urlBase).get().uri("/trips/interno/viajes/activos").retrieve().toBodilessEntity();

        assertThat(correlacion.get()).isEqualTo(actual.toString());
    }

    @Test
    void sinCorrelacionEnviaUnaNueva() {
        clientes.crear(urlBase).get().uri("/trips/interno/viajes/activos").retrieve().toBodilessEntity();

        assertThat(UUID.fromString(correlacion.get())).isNotNull();
    }

    @Test
    void respetaElTiempoDeEspera() {
        RestClient cliente = clientes.crear(urlBase, Duration.ofMillis(300));

        assertThatThrownBy(() -> cliente.get().uri("/lento").retrieve().toBodilessEntity())
                .isInstanceOf(ResourceAccessException.class);
    }

    @Test
    void elTiempoDeEsperaPorDefectoEsDeTresSegundos() {
        assertThat(ClientesInternos.TIEMPO_ESPERA).isEqualTo(Duration.ofSeconds(3));
    }
}
