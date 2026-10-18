package gt.cunoc.proyecto2.sales.integracion.soporte;

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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class PruebaIntegracion {

    /** Fecha de pago elegida por el usuario en las pruebas (no se valida contra hoy). */
    protected static final LocalDate FECHA_PAGO = LocalDate.of(2026, 11, 1);
    protected static final OffsetDateTime SALIDA = OffsetDateTime.parse("2026-11-20T06:00:00-06:00");

    /** Cola de la prueba enlazada a sales.eventos con "#": recibe todo lo que Sales publica. */
    protected static final String COLA_ESPIA = "pruebas.sales.eventos";
    protected static final String DLQ = "sales.q.dlq";
    protected static final String TRIPS_EVENTOS = "trips.eventos";
    protected static final String IDENTITY_EVENTOS = "identity.eventos";
    protected static final String WALLET_EVENTOS = "wallet.eventos";

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    private static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4-management");

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
        registro.add("app.servicios.trips-url", TRIPS::url);
        // La tarea de reservas vencidas no corre sola: las pruebas la llaman cuando la necesitan
        registro.add("app.ventas.liberar-reservas-ms", () -> "3600000");
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
                .requestFactory(new JdkClientHttpRequestFactory())
                .defaultStatusHandler(status -> true, (peticion, respuesta) -> { })
                .build();

        jdbc.execute("""
                TRUNCATE TABLE boletos, ventas, asientos_viaje, vista_viajes, vista_clientes, ordenes_alquiler,
                               eventos_salida, eventos_procesados CASCADE""");

        Queue espia = new Queue(COLA_ESPIA, true, false, false);      // durable: RabbitMQ 4 desaconseja colas transitorias
        amqp.declareQueue(espia);
        amqp.declareBinding(BindingBuilder.bind(espia).to(new TopicExchange("sales.eventos")).with("#"));
        amqp.purgeQueue(COLA_ESPIA, false);
        amqp.purgeQueue(DLQ, false);

        TRIPS.reiniciar();
        circuitos.circuitBreaker("trips").reset();
    }

    // ----- Tokens (firmados con el mismo secreto que valida Sales) -----

    protected String token(UUID usuarioId, String rol, UUID sucursalId) {
        return jwt.generar(new UsuarioActual(usuarioId, rol.toLowerCase(Locale.ROOT) + "@sales.gt", Set.of(rol),
                sucursalId, null));
    }

    protected String tokenAdminSistema() {
        return token(UUID.randomUUID(), Roles.ADMIN_SISTEMA, null);
    }

    protected String tokenAdminSucursal(UUID sucursalId) {
        return token(UUID.randomUUID(), Roles.ADMIN_SUCURSAL, sucursalId);
    }

    protected String tokenCajero(UUID sucursalId) {
        return token(UUID.randomUUID(), Roles.CAJERO, sucursalId);
    }

    protected String tokenCliente(UUID usuarioId) {
        return token(usuarioId, Roles.CLIENTE, null);
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

    /** Número del JSON (JsonPath lo da como Integer o Double) convertido a BigDecimal. */
    protected static BigDecimal decimal(String cuerpo, String ruta) {
        Number numero = JsonPath.read(cuerpo, ruta);
        return new BigDecimal(numero.toString());
    }

    protected static BigDecimal decimal(ResponseEntity<String> respuesta, String ruta) {
        return decimal(respuesta.getBody(), ruta);
    }

    protected static void esStatus(ResponseEntity<String> respuesta, int status) {
        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(status);
    }

    protected static void esError(ResponseEntity<String> respuesta, int status, String codigo) {
        esStatus(respuesta, status);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.codigo")).isEqualTo(codigo);
    }

    // ----- Trips falso (R2) -----

    protected static String rutaVendible(UUID viajeId) {
        return "/trips/interno/viajes/" + viajeId + "/vendible";
    }

    /** Trips responde que el viaje se puede vender (o no, con el motivo). */
    protected void tripsVendible(UUID viajeId, boolean vendible, String motivo) {
        TRIPS.responder("GET", rutaVendible(viajeId), 200, """
                {"viajeId":"%s","vendible":%s,"motivo":%s,"estado":"PROGRAMADO","tipo":"REGULAR",
                 "fechaSalidaProgramada":"2026-11-20T06:00:00-06:00","precioBoleto":75.00,"capacidad":40,
                 "sucursalOrigenId":null}
                """.formatted(viajeId, vendible, motivo == null ? "null" : "\"" + motivo + "\""));
    }

    protected void tripsVendible(UUID viajeId) {
        tripsVendible(viajeId, true, null);
    }

    // ----- Eventos -----

    /** Espera (máx. 10 s) a que Sales publique esos eventos y devuelve el último sobre JSON de cada tipo. */
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
        assertThat(recibidos).as("eventos publicados por Sales").containsKeys(tipos);
        return recibidos;
    }

    /** Todos los sobres que llegan a la cola espía durante unos milisegundos (para contar repetidos). */
    protected List<String> recibirDurante(long milisegundos) {
        List<String> sobres = new ArrayList<>();
        long limite = System.currentTimeMillis() + milisegundos;
        while (System.currentTimeMillis() < limite) {
            Message mensaje = rabbit.receive(COLA_ESPIA, 200);
            if (mensaje != null) {
                sobres.add(new String(mensaje.getBody(), StandardCharsets.UTF_8));
            }
        }
        return sobres;
    }

    /** Publica un evento de otro servicio (sobre completo de comun) como lo haría su relay. */
    protected UUID publicarEvento(String exchange, String tipo, UUID agregadoId, Long versionAgregado,
                                  String payloadJson) {
        UUID eventId = UUID.randomUUID();
        publicarEvento(exchange, tipo, eventId, agregadoId, versionAgregado, payloadJson);
        return eventId;
    }

    protected void publicarEvento(String exchange, String tipo, UUID eventId, UUID agregadoId, Long versionAgregado,
                                  String payloadJson) {
        String sobre = """
                {"eventId":"%s","eventType":"%s","version":1,"aggregateId":"%s","aggregateVersion":%s,
                 "occurredAt":"2026-11-01T14:00:00Z","businessDate":"%s","correlationId":"%s","payload":%s}
                """.formatted(eventId, tipo, agregadoId, versionAgregado == null ? "null" : versionAgregado,
                FECHA_PAGO, UUID.randomUUID(), payloadJson);
        publicarCrudo(exchange, tipo, sobre);
    }

    protected void publicarCrudo(String exchange, String routingKey, String cuerpo) {
        rabbit.send(exchange, routingKey, MessageBuilder.withBody(cuerpo.getBytes(StandardCharsets.UTF_8))
                .setContentType(MediaType.APPLICATION_JSON_VALUE)
                .build());
    }

    /** true si comun ya registró el evento en eventos_procesados (el manejador terminó). */
    protected boolean procesado(UUID eventId) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM eventos_procesados WHERE evento_id = ?",
                Integer.class, eventId);
        return total != null && total == 1;
    }

    // ----- Payloads de Wallet (respuestas de la saga) -----

    /** CarteraDebitada o CarteraReembolsada: referenciaTipo VENTA o ALQUILER. */
    protected static String movimientoCartera(String referenciaTipo, UUID referenciaId, UUID usuarioId,
                                              String monto) {
        return """
                {"movimientoId":"%s","referenciaTipo":"%s","referenciaId":"%s","usuarioId":"%s","monto":%s,
                 "saldoResultante":500.00,"fechaMovimiento":"%s"}
                """.formatted(UUID.randomUUID(), referenciaTipo, referenciaId, usuarioId, monto, FECHA_PAGO);
    }

    protected static String debitoRechazado(String referenciaTipo, UUID referenciaId, UUID usuarioId, String monto,
                                            String motivo) {
        return """
                {"referenciaTipo":"%s","referenciaId":"%s","usuarioId":"%s","monto":%s,"motivo":"%s"}
                """.formatted(referenciaTipo, referenciaId, usuarioId, monto, motivo);
    }

    // ----- Datos de prueba directos en la BD -----

    /** Viaje REGULAR PROGRAMADO (Xela → Guate, Q75, placa P-123ABC) con sus asientos 1..capacidad DISPONIBLE. */
    protected UUID insertarViaje(UUID sucursalOrigenId, int capacidad, String estado) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO vista_viajes (viaje_id, ruta_id, sucursal_origen_id, origen_nombre, destino_nombre,
                                          fecha_salida, bus_id, bus_placa, capacidad, precio_boleto, estado, version)
                VALUES (?, ?, ?, 'Quetzaltenango', 'Guatemala', ?, ?, 'P-123ABC', ?, 75.00, ?, 0)""",
                id, UUID.randomUUID(), sucursalOrigenId, SALIDA, UUID.randomUUID(), capacidad, estado);
        jdbc.update("""
                INSERT INTO asientos_viaje (viaje_id, numero_asiento)
                SELECT ?, n FROM generate_series(1, ?) AS n""", id, capacidad);
        return id;
    }

    protected UUID insertarViaje(UUID sucursalOrigenId) {
        return insertarViaje(sucursalOrigenId, 40, "PROGRAMADO");
    }

    /** Cliente con cuenta en vista_clientes. */
    protected void insertarCliente(UUID usuarioId, String nit, String nombre, String correo) {
        jdbc.update("INSERT INTO vista_clientes (usuario_id, nit, nombre, correo) VALUES (?, ?, ?, ?)",
                usuarioId, nit, nombre, correo);
    }

    /** Orden PENDIENTE_PAGO de Q2600 (Xela → Antigua). clienteUsuarioId null = alquiler de mostrador. */
    protected UUID insertarOrden(UUID sucursalId, UUID clienteUsuarioId, String nit) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO ordenes_alquiler (id, alquiler_id, cliente_usuario_id, cliente_nit, cliente_nombre,
                                              cliente_correo, sucursal_id, origen, destino, monto)
                VALUES (?, ?, ?, ?, 'Colegio San José', 'colegio@correo.com', ?, 'Quetzaltenango',
                        'Antigua Guatemala', 2600.00)""",
                id, UUID.randomUUID(), clienteUsuarioId, nit, sucursalId);
        return id;
    }

    protected String estadoVenta(UUID ventaId) {
        return jdbc.queryForObject("SELECT estado FROM ventas WHERE id = ?", String.class, ventaId);
    }

    protected String estadoOrden(UUID ordenId) {
        return jdbc.queryForObject("SELECT estado FROM ordenes_alquiler WHERE id = ?", String.class, ordenId);
    }

    /** Estado de cada asiento pedido, en orden. */
    protected List<String> estadosAsientos(UUID viajeId, Integer... numeros) {
        List<String> estados = new ArrayList<>();
        for (Integer numero : numeros) {
            estados.add(jdbc.queryForObject(
                    "SELECT estado FROM asientos_viaje WHERE viaje_id = ? AND numero_asiento = ?",
                    String.class, viajeId, numero));
        }
        return estados;
    }
}
