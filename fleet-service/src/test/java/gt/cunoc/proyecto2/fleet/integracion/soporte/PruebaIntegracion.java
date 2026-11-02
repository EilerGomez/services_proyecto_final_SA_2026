package gt.cunoc.proyecto2.fleet.integracion.soporte;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import gt.cunoc.proyecto2.comun.seguridad.service.ServicioJwt;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * Base de las pruebas de integración de Fleet.
 *
 * <ul>
 *   <li>PostgreSQL 16 y RabbitMQ reales en Docker (Testcontainers). Se levantan una sola vez para todas las
 *       clases *IT y Spring reutiliza el mismo contexto.</li>
 *   <li>Fleet corre completo en un puerto aleatorio: filtros de comun, JWT, roles, controladores, servicios,
 *       JPA, Flyway, outbox, relay y consumidor de eventos.</li>
 *   <li>Identity y Trips son {@link ServidorFalso}.</li>
 *   <li>Antes de cada prueba se vacían las tablas, la cola espía y la DLQ.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class PruebaIntegracion {

    protected static final LocalDate FECHA = LocalDate.of(2026, 10, 20);
    /**
     * Almacenamiento de fotos de mentira: un {@link ServidorFalso} que hace de Azure Blob. Así F22 sube
     * de verdad (PUT Blob) y las fotos reemplazadas se borran de verdad (Delete Blob), sin salir a Azure.
     */
    protected static final ServidorFalso BLOBS = ServidorFalso.iniciar();

    /** SAS de mentira: con SAS el cliente autoriza por la URL y no pide token a la identidad de la VM. */
    protected static final String SAS_FOTOS = "sv=2021-08-06&se=2099-01-01&sp=cw&sig=firma-de-prueba";

    protected static final String CUENTA_FOTOS = BLOBS.url();
    protected static final String FOTO_BUS = CUENTA_FOTOS + "/fotos/it/buses/bus.jpg";
    protected static final String FOTO_CHOFER = CUENTA_FOTOS + "/fotos/it/choferes/chofer.jpg";

    /** Cola de la prueba enlazada a fleet.eventos con "#": recibe todo lo que Fleet publica. */
    protected static final String COLA_ESPIA = "pruebas.fleet.eventos";
    protected static final String DLQ = "fleet.q.dlq";

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    private static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4-management");

    protected static final ServidorFalso IDENTITY = ServidorFalso.iniciar();
    protected static final ServidorFalso TRIPS = ServidorFalso.iniciar();

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
        registro.add("app.servicios.identity-url", IDENTITY::url);
        registro.add("app.servicios.trips-url", TRIPS::url);
        registro.add("app.fotos.cuenta-url", () -> CUENTA_FOTOS);
        registro.add("app.fotos.contenedor", () -> "fotos");
        registro.add("app.fotos.prefijo", () -> "it/");
        registro.add("app.fotos.sas", () -> SAS_FOTOS);
    }

    @Autowired
    protected JdbcTemplate jdbc;
    @Autowired
    protected RabbitTemplate rabbit;
    @Autowired
    private AmqpAdmin amqp;
    @Autowired
    private ServicioJwt jwt;
    @Autowired
    private CircuitBreakerRegistry circuitos;
    @Autowired
    private Environment entorno;

    private RestClient http;

    @BeforeEach
    void prepararPruebaIntegracion() {
        // Las respuestas 4xx/5xx no lanzan excepción: la prueba revisa el status y el cuerpo
        http = RestClient.builder()
                .baseUrl("http://localhost:" + entorno.getProperty("local.server.port"))
                .requestFactory(new JdkClientHttpRequestFactory())                // soporta PATCH
                .defaultStatusHandler(status -> true, (peticion, respuesta) -> { })
                .build();

        jdbc.execute("""
                TRUNCATE TABLE bitacora_kilometraje, choferes, buses, personal_sucursal, sucursales,
                               eventos_salida, eventos_procesados CASCADE""");

        Queue espia = new Queue(COLA_ESPIA, true, false, false);      // durable: RabbitMQ 4 desaconseja colas transitorias
        amqp.declareQueue(espia);
        amqp.declareBinding(BindingBuilder.bind(espia).to(new TopicExchange("fleet.eventos")).with("#"));
        amqp.purgeQueue(COLA_ESPIA, false);
        amqp.purgeQueue(DLQ, false);

        IDENTITY.reiniciar();
        TRIPS.reiniciar();
        BLOBS.reiniciar();
        circuitos.circuitBreaker("identity").reset();
        circuitos.circuitBreaker("trips").reset();
    }

    // ----- Tokens (firmados con el mismo secreto que valida Fleet) -----

    protected String tokenAdminSistema() {
        return jwt.generar(new UsuarioActual(UUID.randomUUID(), "admin@fleet.gt", Set.of(Roles.ADMIN_SISTEMA),
                null, null));
    }

    protected String tokenAdminSucursal(UUID sucursalId) {
        return jwt.generar(new UsuarioActual(UUID.randomUUID(), "admin.sucursal@fleet.gt",
                Set.of(Roles.ADMIN_SUCURSAL), sucursalId, null));
    }

    protected String tokenChofer(UUID choferId) {
        return jwt.generar(new UsuarioActual(UUID.randomUUID(), "chofer@fleet.gt", Set.of(Roles.CHOFER),
                null, choferId));
    }

    protected String tokenCliente() {
        return jwt.generar(new UsuarioActual(UUID.randomUUID(), "cliente@fleet.gt", Set.of(Roles.CLIENTE),
                null, null));
    }

    /** Token de otro microservicio (rol SERVICIO), como el que manda Trips en R1a y R1b. */
    protected String tokenServicio() {
        return jwt.generarServicio("trips-service");
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

    /** POST multipart (F22) con un archivo en la parte "archivo". */
    protected ResponseEntity<String> subirArchivo(String ruta, String token, String nombre, byte[] contenido) {
        MultiValueMap<String, Object> partes = new LinkedMultiValueMap<>();
        partes.add("archivo", new ByteArrayResource(contenido) {
            @Override
            public String getFilename() {
                return nombre;
            }
        });
        return http.post().uri(ruta)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(partes)
                .retrieve()
                .toEntity(String.class);
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

    /** Espera (máx. 10 s) a que Fleet publique esos eventos y devuelve el sobre JSON de cada uno. */
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
        assertThat(recibidos).as("eventos publicados por Fleet").containsKeys(tipos);
        return recibidos;
    }

    /** Publica un evento de otro servicio (sobre completo de comun) como lo haría su relay. */
    protected UUID publicarEvento(String exchange, String tipo, UUID agregadoId, String payloadJson) {
        UUID eventId = UUID.randomUUID();
        publicarEvento(exchange, tipo, eventId, agregadoId, payloadJson);
        return eventId;
    }

    protected void publicarEvento(String exchange, String tipo, UUID eventId, UUID agregadoId, String payloadJson) {
        String sobre = """
                {"eventId":"%s","eventType":"%s","version":1,"aggregateId":"%s","aggregateVersion":1,
                 "occurredAt":"2026-10-20T14:00:00Z","businessDate":"%s","correlationId":"%s","payload":%s}
                """.formatted(eventId, tipo, agregadoId, FECHA, UUID.randomUUID(), payloadJson);
        publicarCrudo(exchange, tipo, sobre);
    }

    protected void publicarCrudo(String exchange, String routingKey, String cuerpo) {
        rabbit.send(exchange, routingKey, MessageBuilder.withBody(cuerpo.getBytes(StandardCharsets.UTF_8))
                .setContentType(MediaType.APPLICATION_JSON_VALUE)
                .build());
    }

    // ----- Datos de prueba directos en la BD -----

    protected UUID insertarSucursal(String codigo, boolean activa) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO sucursales (id, codigo, nombre, departamento, municipio, direccion, latitud, longitud, activa)
                VALUES (?, ?, ?, 'Quetzaltenango', 'Quetzaltenango', 'Zona 1', 14.834500, -91.518000, ?)""",
                id, codigo, "Sucursal " + codigo, activa);
        return id;
    }

    protected UUID insertarPersonal(UUID sucursalId, String rol) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO personal_sucursal (id, sucursal_id, usuario_id, nombre_completo, correo, rol_codigo,
                                               fecha_asignacion)
                VALUES (?, ?, ?, 'Ana Lopez', ?, ?, DATE '2026-01-01')""",
                id, sucursalId, UUID.randomUUID(), id + "@fleet.gt", rol);
        return id;
    }

    protected UUID insertarBus(UUID sucursalId, String placa, String estado, String kilometraje) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO buses (id, sucursal_id, placa, marca, modelo, anio_fabricacion, capacidad_pasajeros,
                                   estado, kilometraje_actual, foto_url)
                VALUES (?, ?, ?, 'Volvo', 'B12', 2018, 50, ?, ?, ?)""",
                id, sucursalId, placa, estado, new BigDecimal(kilometraje), FOTO_BUS);
        return id;
    }

    protected UUID insertarChofer(UUID sucursalId, UUID usuarioId, String licencia, LocalDate vencimiento) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO choferes (id, sucursal_id, usuario_id, nombre_completo, numero_licencia, tipo_licencia,
                                      fecha_vencimiento_licencia, salario_base_viaje)
                VALUES (?, ?, ?, 'Juan Perez', ?, 'A', ?, 350.00)""",
                id, sucursalId, usuarioId, licencia, vencimiento);
        return id;
    }

    protected void insertarViajeEnBitacora(UUID busId, String kmInicial, String kmFinal, OffsetDateTime llegada) {
        jdbc.update("""
                INSERT INTO bitacora_kilometraje (bus_id, viaje_id, km_inicial, km_final, fecha_llegada)
                VALUES (?, ?, ?, ?, ?)""",
                busId, UUID.randomUUID(), new BigDecimal(kmInicial), new BigDecimal(kmFinal), llegada);
    }

    protected int contar(String tabla) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
        return total == null ? 0 : total;
    }

    /** Respuesta de R5 de Identity con una cuenta nueva. */
    protected static String cuentaCreada(UUID usuarioId, String correo, String nombreCompleto) {
        return """
                {"usuarioId":"%s","correo":"%s","nombreCompleto":"%s","creada":true}
                """.formatted(usuarioId, correo, nombreCompleto);
    }

    /** Error con el formato de comun, como lo responde otro servicio. */
    protected static String errorRemoto(int status, String codigo, String mensaje) {
        return """
                {"status":%d,"codigo":"%s","mensaje":"%s","ruta":"/remoto","timestamp":"2026-10-20T14:00:00Z",
                 "correlationId":null,"errores":[]}
                """.formatted(status, codigo, mensaje);
    }
}
