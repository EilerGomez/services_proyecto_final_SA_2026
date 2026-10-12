package gt.cunoc.proyecto2.notification.integracion.soporte;

import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@SpringBootTest
public abstract class PruebaIntegracion {

    protected static final String IDENTITY_EVENTOS = "identity.eventos";
    protected static final String FLEET_EVENTOS = "fleet.eventos";
    protected static final String TRIPS_EVENTOS = "trips.eventos";
    protected static final String SALES_EVENTOS = "sales.eventos";

    private static final int SMTP = 1025;
    private static final int API = 8025;

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    private static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4-management");
    private static final GenericContainer<?> MAILPIT = new GenericContainer<>("axllent/mailpit:v1.20.0")
            .withExposedPorts(SMTP, API);

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    static {
        POSTGRES.start();
        RABBIT.start();
        MAILPIT.start();
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
        registro.add("spring.mail.host", MAILPIT::getHost);
        registro.add("spring.mail.port", () -> MAILPIT.getMappedPort(SMTP));
    }

    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected RabbitTemplate rabbit;

    @BeforeEach
    void prepararPruebaIntegracion() {
        jdbc.execute("TRUNCATE TABLE notificaciones, recordatorios_licencia, eventos_procesados");
        borrarCorreos();
    }

    // ----- Eventos -----

    /** Publica un evento de otro servicio (sobre completo de comun) como lo haría su relay. */
    protected UUID publicarEvento(String exchange, String tipo, String fechaNegocio, String payloadJson) {
        UUID eventId = UUID.randomUUID();
        publicarEvento(exchange, tipo, eventId, fechaNegocio, payloadJson);
        return eventId;
    }

    protected void publicarEvento(String exchange, String tipo, UUID eventId, String fechaNegocio,
                                  String payloadJson) {
        String sobre = """
                {"eventId":"%s","eventType":"%s","version":1,"aggregateId":"%s","aggregateVersion":null,
                 "occurredAt":"2026-11-01T14:00:00Z","businessDate":"%s","correlationId":"%s","payload":%s}
                """.formatted(eventId, tipo, UUID.randomUUID(), fechaNegocio, UUID.randomUUID(), payloadJson);
        rabbit.send(exchange, tipo, MessageBuilder.withBody(sobre.getBytes(StandardCharsets.UTF_8))
                .setContentType(MediaType.APPLICATION_JSON_VALUE)
                .build());
    }

    /** true si comun ya registró el evento en eventos_procesados (el manejador terminó sin error). */
    protected boolean procesado(UUID eventId) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM eventos_procesados WHERE evento_id = ?",
                Integer.class, eventId);
        return total != null && total == 1;
    }

    // ----- Tabla notificaciones -----

    /** Filas de notificaciones de un evento (plantilla, destinatario, asunto, estado, intentos, datos). */
    protected List<Map<String, Object>> notificaciones(UUID eventId) {
        return jdbc.queryForList("""
                SELECT plantilla, destinatario, asunto, estado, intentos, datos::text AS datos, fecha_envio
                FROM notificaciones WHERE evento_id = ?""", eventId);
    }

    protected int totalNotificaciones() {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM notificaciones", Integer.class);
        return total == null ? 0 : total;
    }

    // ----- Mailpit (API HTTP) -----

    /** Correos recibidos en Mailpit, como JSON de /api/v1/messages. */
    protected String buzon() {
        return llamar("GET", "/api/v1/messages");
    }

    protected int totalCorreos() {
        return JsonPath.<Integer>read(buzon(), "$.total");
    }

    /** Asuntos de los correos que llegaron a ese destinatario. */
    protected List<String> asuntosPara(String correo) {
        return JsonPath.read(buzon(), "$.messages[?(@.To[0].Address == '" + correo + "')].Subject");
    }

    /** Texto del correo más reciente para ese destinatario. */
    protected String textoPara(String correo) {
        List<String> ids = JsonPath.read(buzon(), "$.messages[?(@.To[0].Address == '" + correo + "')].ID");
        if (ids.isEmpty()) {
            throw new AssertionError("No llegó ningún correo para " + correo);
        }
        return JsonPath.read(llamar("GET", "/api/v1/message/" + ids.get(0)), "$.Text");
    }

    private void borrarCorreos() {
        llamar("DELETE", "/api/v1/messages");
    }

    private static String llamar(String metodo, String ruta) {
        URI uri = URI.create("http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(API) + ruta);
        HttpRequest peticion = HttpRequest.newBuilder(uri)
                .method(metodo, HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            return HTTP.send(peticion, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).body();
        } catch (IOException e) {
            throw new IllegalStateException("Mailpit no respondió", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrumpido esperando a Mailpit", e);
        }
    }
}