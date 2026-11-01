package gt.cunoc.proyecto2.wallet.clientes;

import com.sun.net.httpserver.HttpServer;
import gt.cunoc.proyecto2.comun.cliente.ClientesInternos;
import gt.cunoc.proyecto2.comun.errores.ServicioNoDisponibleException;
import gt.cunoc.proyecto2.wallet.config.ConfiguracionCircuitos;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** R4c contra un Identity falso (servidor HTTP del JDK): activa, inactiva, no existe y caído. */
class ClienteIdentityTest {

    private HttpServer identity;
    private String url;
    private final AtomicInteger status = new AtomicInteger(200);
    private final AtomicReference<String> estado = new AtomicReference<>("ACTIVO");
    private final AtomicReference<String> rutaRecibida = new AtomicReference<>();

    @BeforeEach
    void levantarIdentityFalso() throws IOException {
        identity = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        identity.createContext("/", intercambio -> {
            rutaRecibida.set(intercambio.getRequestURI().getPath());
            String id = rutaRecibida.get().replaceAll(".*/usuarios/([^/]+)/estado", "$1");
            byte[] cuerpo = (status.get() == 200
                    ? "{\"usuarioId\":\"" + id + "\",\"estado\":\"" + estado.get() + "\",\"bloqueadoHasta\":null}"
                    : "{\"codigo\":\"X\"}").getBytes(StandardCharsets.UTF_8);
            intercambio.getResponseHeaders().add("Content-Type", "application/json");
            intercambio.sendResponseHeaders(status.get(), cuerpo.length);
            try (OutputStream salida = intercambio.getResponseBody()) {
                salida.write(cuerpo);
            }
        });
        identity.start();
        url = "http://127.0.0.1:" + identity.getAddress().getPort();
    }

    @AfterEach
    void detener() {
        identity.stop(0);
    }

    private ClienteIdentity cliente() {
        return new ClienteIdentity(RestClient.builder().baseUrl(url).build());
    }

    @Test
    void cuentaActivaLlamaAR4c() {
        UUID usuarioId = UUID.randomUUID();

        assertThat(cliente().cuentaActiva(usuarioId)).isTrue();
        assertThat(rutaRecibida.get()).isEqualTo("/identity/interno/usuarios/" + usuarioId + "/estado");
    }

    @Test
    void cuentaInactivaOBloqueadaNoEstaActiva() {
        estado.set("BLOQUEADO");
        assertThat(cliente().cuentaActiva(UUID.randomUUID())).isFalse();

        estado.set("INACTIVO");
        assertThat(cliente().cuentaActiva(UUID.randomUUID())).isFalse();
    }

    @Test
    void cuentaQueNoExisteNoEstaActiva() {
        status.set(404);

        assertThat(cliente().cuentaActiva(UUID.randomUUID())).isFalse();
    }

    @Test
    void errorDeIdentityEsServicioNoDisponible() {
        status.set(500);

        assertThatThrownBy(() -> cliente().cuentaActiva(UUID.randomUUID()))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .extracting("codigo").isEqualTo("IDENTITY_NO_DISPONIBLE");
    }

    @Test
    void identityApagadoEsServicioNoDisponible() throws IOException {
        int puertoLibre;
        try (ServerSocket socket = new ServerSocket(0)) {
            puertoLibre = socket.getLocalPort();
        }
        ClienteIdentity apagado = new ClienteIdentity(
                RestClient.builder().baseUrl("http://127.0.0.1:" + puertoLibre).build());

        assertThatThrownBy(() -> apagado.cuentaActiva(UUID.randomUUID()))
                .isInstanceOf(ServicioNoDisponibleException.class);
    }

    @Test
    void constructorDeSpringUsaClientesInternos() {
        ClientesInternos internos = mock(ClientesInternos.class);
        when(internos.crear(url)).thenReturn(RestClient.builder().baseUrl(url).build());

        assertThat(new ClienteIdentity(internos, url,
                CircuitBreakerRegistry.of(ConfiguracionCircuitos.configuracion()))
                .cuentaActiva(UUID.randomUUID())).isTrue();
    }

    @Test
    void tras5FallasElCircuitoSeAbreYYaNoLlamaAIdentity() {
        // Arrange: Identity responde 500 y el mínimo configurado para evaluar son 5 llamadas
        status.set(500);
        ClienteIdentity cliente = cliente();
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> cliente.cuentaActiva(UUID.randomUUID()))
                    .isInstanceOf(ServicioNoDisponibleException.class);
        }

        // Act: la siguiente ya no debería salir a la red
        rutaRecibida.set(null);
        assertThatThrownBy(() -> cliente.cuentaActiva(UUID.randomUUID()))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .extracting("codigo").isEqualTo("IDENTITY_NO_DISPONIBLE");

        // Assert: el circuito la cortó, Identity no recibió nada
        assertThat(rutaRecibida.get()).isNull();
    }

    @Test
    void unaCuentaQueNoExisteNoAbreElCircuito() {
        // Arrange: 404 está en ignoreExceptions, no es una caída del servicio
        status.set(404);
        ClienteIdentity cliente = cliente();
        for (int i = 0; i < 10; i++) {
            assertThat(cliente.cuentaActiva(UUID.randomUUID())).isFalse();
        }

        // Act + Assert: el circuito sigue cerrado y la siguiente llamada pasa normal
        status.set(200);
        assertThat(cliente.cuentaActiva(UUID.randomUUID())).isTrue();
    }

    @Test
    void elCircuitoAbiertoCortaSinEsperarAIdentity() {
        // Arrange: un circuito ya abierto a mano, para no depender de las 5 fallas
        CircuitBreaker abierto = CircuitBreakerRegistry.of(ConfiguracionCircuitos.configuracion())
                .circuitBreaker("identity");
        abierto.transitionToOpenState();
        ClienteIdentity cliente = new ClienteIdentity(RestClient.builder().baseUrl(url).build(), abierto);

        // Act + Assert
        assertThatThrownBy(() -> cliente.cuentaActiva(UUID.randomUUID()))
                .isInstanceOf(ServicioNoDisponibleException.class)
                .extracting("codigo").isEqualTo("IDENTITY_NO_DISPONIBLE");
        assertThat(rutaRecibida.get()).isNull();
    }

    @Test
    void estadoCuentaSoloAceptaActivo() {
        assertThat(new EstadoCuenta(UUID.randomUUID(), "ACTIVO").activa()).isTrue();
        assertThat(new EstadoCuenta(UUID.randomUUID(), null).activa()).isFalse();
    }
}
