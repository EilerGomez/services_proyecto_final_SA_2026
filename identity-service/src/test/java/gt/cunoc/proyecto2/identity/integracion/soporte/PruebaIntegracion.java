package gt.cunoc.proyecto2.identity.integracion.soporte;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import gt.cunoc.proyecto2.comun.seguridad.service.ServicioJwt;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.identity.models.Rol;
import gt.cunoc.proyecto2.identity.models.Usuario;
import gt.cunoc.proyecto2.identity.repositories.RolRepository;
import gt.cunoc.proyecto2.identity.repositories.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * Base de las pruebas de integración de Identity.
 *
 * <ul>
 *   <li>PostgreSQL 16 y RabbitMQ reales en Docker (Testcontainers). Se levantan una sola vez para todas las
 *       clases *IT y Spring reutiliza el mismo contexto.</li>
 *   <li>Identity corre completo en un puerto aleatorio: filtros de comun, JWT, roles, controladores, servicios,
 *       JPA, Flyway, outbox, relay y consumidor de eventos.</li>
 *   <li>Cola espía enlazada a identity.eventos con "#": recibe todo lo que Identity publica.</li>
 *   <li>Antes de cada prueba se vacían las tablas y la cola espía, y se crea un ADMIN_SISTEMA.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class PruebaIntegracion {

    protected static final LocalDate FECHA = LocalDate.of(2026, 10, 20);
    protected static final String CONTRASENA = "Segura#2026";

    /** Cola de la prueba enlazada a identity.eventos con "#": recibe todo lo que Identity publica. */
    protected static final String COLA_ESPIA = "pruebas.identity.eventos";
    protected static final String DLQ = "identity.q.dlq";

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    private static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4-management");

    static {
        POSTGRES.start();
        RABBIT.start();
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
    protected UsuarioRepository usuarios;
    @Autowired
    protected RolRepository roles;
    @Autowired
    protected PasswordEncoder claves;
    @Autowired
    private Environment entorno;

    protected UUID adminId;
    protected String tokenAdmin;

    private RestClient http;

    @BeforeEach
    void prepararPruebaIntegracion() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + entorno.getProperty("local.server.port"))
                .requestFactory(new JdkClientHttpRequestFactory())
                .defaultStatusHandler(status -> true, (peticion, respuesta) -> { })
                .build();

        jdbc.execute("""
                TRUNCATE TABLE codigos_doble_factor, movimientos_cartera, carteras, tokens_recuperacion,
                                tokens_refresco, perfiles, usuario_roles, usuarios, solicitudes_cuenta_personal,
                                tarifas_depreciacion, eventos_salida, eventos_procesados CASCADE""");

        Queue espia = new Queue(COLA_ESPIA, true, false, false);
        amqp.declareQueue(espia);
        amqp.declareBinding(BindingBuilder.bind(espia).to(new TopicExchange("identity.eventos")).with("#"));
        amqp.purgeQueue(COLA_ESPIA, false);
        amqp.purgeQueue(DLQ, false);

        Usuario admin = new Usuario();
        admin.setCorreo("admin@pruebas.gt");
        admin.setClaveCifrada(claves.encode(CONTRASENA));
        admin.setEstado("ACTIVO");
        Rol sistema = roles.findById(Roles.ADMIN_SISTEMA).orElseThrow();
        Rol cliente = roles.findById(Roles.CLIENTE).orElseThrow();
        admin.getRoles().add(sistema);
        admin.getRoles().add(cliente);
        usuarios.save(admin);
        adminId = admin.getId();
        tokenAdmin = jwt.generar(new UsuarioActual(adminId, admin.getCorreo(),
                Set.of(Roles.ADMIN_SISTEMA, Roles.CLIENTE), null, null));
    }

    // ----- Tokens -----

    protected String tokenCliente(UUID usuarioId, String correo) {
        return jwt.generar(new UsuarioActual(usuarioId, correo, Set.of(Roles.CLIENTE), null, null));
    }

    /** Token de otro microservicio (rol SERVICIO), como el que manda Fleet en R5. */
    protected String tokenServicio() {
        return jwt.generarServicio("fleet-service");
    }

    // ----- HTTP -----

    protected ResponseEntity<String> get(String ruta, String token) {
        return enviar(HttpMethod.GET, ruta, token, null);
    }

    protected ResponseEntity<String> post(String ruta, String token, String json) {
        return enviar(HttpMethod.POST, ruta, token, json);
    }

    protected ResponseEntity<String> put(String ruta, String token, String json) {
        return enviar(HttpMethod.PUT, ruta, token, json);
    }

    protected ResponseEntity<String> patch(String ruta, String token, String json) {
        return enviar(HttpMethod.PATCH, ruta, token, json);
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

    // ----- Eventos -----

    /** Espera (máx. 10 s) a que Identity publique esos eventos y devuelve el sobre JSON de cada uno. */
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
        assertThat(recibidos).as("eventos publicados por Identity").containsKeys(tipos);
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

    /** Registra un cliente por HTTP y devuelve su id. */
    protected UUID registrarCliente(String correo, String nit, String dpi) {
        ResponseEntity<String> respuesta = post("/identity/auth/registro", null, """
                {"correo":"%s","contrasena":"%s","nombres":"Ana","apellidos":"López",
                 "nit":"%s","dpi":"%s","telefono":"55551234","direccion":"Xela"}
                """.formatted(correo, CONTRASENA, nit, dpi));
        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(201);
        return UUID.fromString(json(respuesta, "$.usuarioId"));
    }

    protected int contar(String tabla) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
        return total == null ? 0 : total;
    }
}
