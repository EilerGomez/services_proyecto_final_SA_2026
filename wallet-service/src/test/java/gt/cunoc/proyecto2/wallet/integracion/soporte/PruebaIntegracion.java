package gt.cunoc.proyecto2.wallet.integracion.soporte;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.sun.net.httpserver.HttpServer;
import gt.cunoc.proyecto2.comun.seguridad.service.ServicioJwt;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * Base de las pruebas de integración de Wallet.
 *
 * <ul>
 *   <li>PostgreSQL 16 y RabbitMQ reales en Docker (Testcontainers), una sola vez para todas las clases *IT.</li>
 *   <li>Wallet corre completo en un puerto aleatorio: filtros de comun, JWT, roles, controlador, servicios,
 *       JPA, Flyway, outbox, relay y consumidor de eventos.</li>
 *   <li>Identity falso (servidor HTTP del JDK) para R4c: por defecto toda cuenta está ACTIVA;
 *       {@link #estadoEnIdentity} cambia el estado de una.</li>
 *   <li>Cola espía enlazada a wallet.eventos con "#": recibe todo lo que Wallet publica.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class PruebaIntegracion {

    protected static final LocalDate FECHA = LocalDate.of(2026, 10, 20);
    protected static final String SALES_EVENTOS = "sales.eventos";
    protected static final String IDENTITY_EVENTOS = "identity.eventos";

    /** Cola de la prueba enlazada a wallet.eventos con "#": recibe todo lo que Wallet publica. */
    protected static final String COLA_ESPIA = "pruebas.wallet.eventos";
    protected static final String DLQ = "wallet.q.dlq";

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    private static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4-management");

    /** Estado de cada cuenta en el Identity falso. Sin entrada = ACTIVO; "NO_EXISTE" = 404. */
    private static final Map<UUID, String> ESTADOS = new ConcurrentHashMap<>();
    private static final HttpServer IDENTITY = identityFalso();

    static {
        POSTGRES.start();
        RABBIT.start();
        IDENTITY.start();
    }

    private static HttpServer identityFalso() {
        try {
            HttpServer servidor = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            servidor.createContext("/identity/interno/usuarios/", intercambio -> {
                String ruta = intercambio.getRequestURI().getPath();
                UUID id = UUID.fromString(ruta.replaceAll(".*/usuarios/([^/]+)/estado", "$1"));
                String estado = ESTADOS.getOrDefault(id, "ACTIVO");
                int status = "NO_EXISTE".equals(estado) ? 404 : 200;
                byte[] cuerpo = (status == 200
                        ? "{\"usuarioId\":\"" + id + "\",\"estado\":\"" + estado + "\",\"bloqueadoHasta\":null}"
                        : "{\"status\":404,\"codigo\":\"USUARIO_NO_EXISTE\"}").getBytes(StandardCharsets.UTF_8);
                intercambio.getResponseHeaders().add("Content-Type", "application/json");
                intercambio.sendResponseHeaders(status, cuerpo.length);
                try (OutputStream salida = intercambio.getResponseBody()) {
                    salida.write(cuerpo);
                }
            });
            return servidor;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @DynamicPropertySource
    static void propiedades(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
        registro.add("spring.rabbitmq.host", RABBIT::getHost);
        registro.add("spring.rabbitmq.port", RABBIT::getAmqpPort);
        registro.add("spring.rabbitmq.username", RABBIT::getAdminUsername);
        registro.add("spring.rabbitmq.password", RABBIT::getAdminPassword);
        registro.add("spring.rabbitmq.listener.simple.retry.initial-interval", () -> "100");
        registro.add("comun.eventos.outbox.intervalo-ms", () -> "200");
        registro.add("app.jwt.secret", () -> "secreto-de-pruebas-de-al-menos-32-caracteres-00");
        registro.add("app.servicios.identity-url", () -> "http://127.0.0.1:" + IDENTITY.getAddress().getPort());
    }

    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected RabbitTemplate rabbit;
    @Autowired
    private AmqpAdmin amqp;
    @Autowired
    protected ServicioJwt jwt;
    @Autowired
    private Environment entorno;

    private RestClient http;

    @BeforeEach
    void prepararPruebaIntegracion() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + entorno.getProperty("local.server.port"))
                .requestFactory(new JdkClientHttpRequestFactory())
                .defaultStatusHandler(status -> true, (peticion, respuesta) -> { })
                .build();

        jdbc.execute("TRUNCATE TABLE movimientos_cartera, carteras, eventos_salida, eventos_procesados CASCADE");
        ESTADOS.clear();

        Queue espia = new Queue(COLA_ESPIA, true, false, false);
        amqp.declareQueue(espia);
        amqp.declareBinding(BindingBuilder.bind(espia).to(new TopicExchange("wallet.eventos")).with("#"));
        amqp.purgeQueue(COLA_ESPIA, false);
        amqp.purgeQueue(DLQ, false);
    }

    // ----- Identity falso -----

    protected void estadoEnIdentity(UUID usuarioId, String estado) {
        ESTADOS.put(usuarioId, estado);
    }

    // ----- Tokens -----

    protected String tokenCliente(UUID usuarioId) {
        return jwt.generar(new UsuarioActual(usuarioId, "cliente@pruebas.gt", Set.of(Roles.CLIENTE), null, null));
    }

    protected String tokenCajero(UUID usuarioId) {
        return jwt.generar(new UsuarioActual(usuarioId, "cajero@pruebas.gt", Set.of(Roles.CAJERO),
                UUID.randomUUID(), null));
    }

    // ----- HTTP -----

    protected ResponseEntity<String> get(String ruta, String token) {
        return enviar(HttpMethod.GET, ruta, token, null);
    }

    protected ResponseEntity<String> post(String ruta, String token, String json) {
        return enviar(HttpMethod.POST, ruta, token, json);
    }

    private ResponseEntity<String> enviar(HttpMethod metodo, String ruta, String token, String json) {
        RestClient.RequestBodySpec peticion = http.method(metodo).uri(ruta);
        if (token != null) {
            peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        if (json != null) {
            peticion.contentType(MediaType.APPLICATION_JSON).body(json);
        }
        return peticion.retrieve().toEntity(String.class);
    }

    /** Lee un valor del JSON de la respuesta, p. ej. {@code json(r, "$.codigo")}. */
    protected static <T> T json(ResponseEntity<String> respuesta, String ruta) {
        return JsonPath.read(respuesta.getBody(), ruta);
    }

    protected static void esError(ResponseEntity<String> respuesta, int status, String codigo) {
        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(status);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.codigo")).isEqualTo(codigo);
    }

    /** Recarga por HTTP (pasarela simulada) y verifica 201. */
    protected void recargar(UUID usuarioId, String monto) {
        ResponseEntity<String> respuesta = post("/wallet/cartera/recargas", tokenCliente(usuarioId),
                "{\"monto\":" + monto + ",\"fechaMovimiento\":\"" + FECHA + "\"}");
        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(201);
    }

    // ----- Eventos -----

    /** Espera (máx. 10 s) a que Wallet publique esos eventos y devuelve el sobre JSON de cada uno. */
    protected Map<String, String> esperarEventos(String... tipos) {
        Map<String, String> recibidos = new HashMap<>();
        long limite = System.currentTimeMillis() + 10_000;
        while (!recibidos.keySet().containsAll(List.of(tipos)) && System.currentTimeMillis() < limite) {
            Message mensaje = rabbit.receive(COLA_ESPIA, 500);
            if (mensaje != null) {
                String sobre = new String(mensaje.getBody(), StandardCharsets.UTF_8);
                recibidos.put(JsonPath.read(sobre, "$.eventType"), sobre);
            }
        }
        assertThat(recibidos).as("eventos publicados por Wallet").containsKeys(tipos);
        return recibidos;
    }

    /** Publica un evento de otro servicio (sobre completo de comun) como lo haría su relay. */
    protected UUID publicarEvento(String exchange, String tipo, UUID agregadoId, String payloadJson) {
        UUID eventId = UUID.randomUUID();
        String sobre = """
                {"eventId":"%s","eventType":"%s","version":1,"aggregateId":"%s","aggregateVersion":1,
                 "occurredAt":"2026-10-20T14:00:00Z","businessDate":"%s","correlationId":"%s","payload":%s}
                """.formatted(eventId, tipo, agregadoId, FECHA, UUID.randomUUID(), payloadJson);
        rabbit.send(exchange, tipo, MessageBuilder.withBody(sobre.getBytes(StandardCharsets.UTF_8))
                .setContentType(MediaType.APPLICATION_JSON_VALUE)
                .build());
        return eventId;
    }

    protected int contar(String tabla) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
        return total == null ? 0 : total;
    }
}
