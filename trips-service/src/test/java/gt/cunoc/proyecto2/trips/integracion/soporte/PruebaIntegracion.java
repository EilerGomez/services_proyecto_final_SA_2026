package gt.cunoc.proyecto2.trips.integracion.soporte;

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
import java.time.ZoneId;
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

/**
 * Base de las pruebas de integración de Trips.
 *
 * <ul>
 *   <li>PostgreSQL 16 y RabbitMQ 4 reales en Docker (Testcontainers). Se levantan una sola vez para todas
 *       las clases *IT y Spring reutiliza el mismo contexto.</li>
 *   <li>Trips corre completo en un puerto aleatorio: filtros de comun, JWT, roles, controladores, servicios,
 *       JPA, Flyway, outbox, relay y consumidor de eventos.</li>
 *   <li>Fleet (R1) es un {@link ServidorFalso}.</li>
 *   <li>Antes de cada prueba se vacían las tablas, la cola espía y la DLQ.</li>
 * </ul>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class PruebaIntegracion {

    /** Día y horario de los viajes de prueba (las fechas de viaje no se validan contra hoy). */
    protected static final LocalDate DIA = LocalDate.of(2026, 11, 20);
    protected static final OffsetDateTime SALIDA = OffsetDateTime.parse("2026-11-20T06:00:00-06:00");
    protected static final OffsetDateTime LLEGADA = OffsetDateTime.parse("2026-11-20T10:00:00-06:00");
    /** Salida de los alquileres de prueba. */
    protected static final OffsetDateTime SALIDA_ALQUILER = OffsetDateTime.parse("2026-12-20T07:00:00-06:00");

    /** Cola de la prueba enlazada a trips.eventos con "#": recibe todo lo que Trips publica. */
    protected static final String COLA_ESPIA = "pruebas.trips.eventos";
    protected static final String DLQ = "trips.q.dlq";

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    private static final RabbitMQContainer RABBIT = new RabbitMQContainer("rabbitmq:4-management");

    protected static final ServidorFalso FLEET = ServidorFalso.iniciar();

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
        registro.add("app.servicios.fleet-url", FLEET::url);
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

        // TRUNCATE no dispara los triggers de fila (fn_registro_inmutable, fn_eliminar_solo_programado)
        jdbc.execute("""
                TRUNCATE TABLE llegadas_viaje, salidas_viaje, viajes, alquileres_privados, rutas,
                               configuracion_alquiler, vista_tarifas_depreciacion, vista_choferes, vista_buses,
                               vista_sucursales, eventos_salida, eventos_procesados CASCADE""");

        Queue espia = new Queue(COLA_ESPIA, true, false, false);      // durable: RabbitMQ 4 desaconseja colas transitorias
        amqp.declareQueue(espia);
        amqp.declareBinding(BindingBuilder.bind(espia).to(new TopicExchange("trips.eventos")).with("#"));
        amqp.purgeQueue(COLA_ESPIA, false);
        amqp.purgeQueue(DLQ, false);

        FLEET.reiniciar();
        circuitos.circuitBreaker("fleet").reset();
    }

    /** Hoy en Guatemala: T16 rechaza fechas de solicitud anteriores. */
    protected static LocalDate hoy() {
        return LocalDate.now(ZoneId.of("America/Guatemala"));
    }

    // ----- Tokens (firmados con el mismo secreto que valida Trips) -----

    protected String token(UUID usuarioId, String rol, UUID sucursalId, UUID choferId) {
        return jwt.generar(new UsuarioActual(usuarioId, rol.toLowerCase(Locale.ROOT) + "@trips.gt", Set.of(rol),
                sucursalId, choferId));
    }

    protected String tokenAdminSistema() {
        return token(UUID.randomUUID(), Roles.ADMIN_SISTEMA, null, null);
    }

    protected String tokenAdminSucursal(UUID sucursalId) {
        return token(UUID.randomUUID(), Roles.ADMIN_SUCURSAL, sucursalId, null);
    }

    protected String tokenCajero(UUID sucursalId) {
        return token(UUID.randomUUID(), Roles.CAJERO, sucursalId, null);
    }

    protected String tokenChofer(UUID choferId) {
        return token(UUID.randomUUID(), Roles.CHOFER, null, choferId);
    }

    /** Correo del token: "cliente@trips.gt". */
    protected String tokenCliente(UUID usuarioId) {
        return token(usuarioId, Roles.CLIENTE, null, null);
    }

    /** Token de otro microservicio (rol SERVICIO), como el que manda Fleet en R3 o Sales en R2. */
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

    protected ResponseEntity<String> delete(String ruta, String token) {
        return enviar(HttpMethod.DELETE, ruta, token, null);
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

    /** Fecha con zona del JSON; equals de OffsetDateTime también compara el offset (-06:00). */
    protected static OffsetDateTime fecha(String cuerpo, String ruta) {
        return OffsetDateTime.parse(JsonPath.<String>read(cuerpo, ruta));
    }

    protected static OffsetDateTime fecha(ResponseEntity<String> respuesta, String ruta) {
        return fecha(respuesta.getBody(), ruta);
    }

    protected static void esStatus(ResponseEntity<String> respuesta, int status) {
        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(status);
    }

    protected static void esError(ResponseEntity<String> respuesta, int status, String codigo) {
        esStatus(respuesta, status);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.codigo")).isEqualTo(codigo);
    }

    // ----- Eventos -----

    /** Espera (máx. 10 s) a que Trips publique esos eventos y devuelve el sobre JSON de cada uno. */
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
        assertThat(recibidos).as("eventos publicados por Trips").containsKeys(tipos);
        return recibidos;
    }

    /** aggregateVersion del sobre como int. */
    protected static int versionDelEvento(String sobre) {
        return JsonPath.<Number>read(sobre, "$.aggregateVersion").intValue();
    }

    /** Publica un evento de otro servicio (sobre completo de comun) como lo haría su relay. */
    protected UUID publicarEvento(String exchange, String tipo, UUID agregadoId, long versionAgregado,
                                  String payloadJson) {
        UUID eventId = UUID.randomUUID();
        publicarEvento(exchange, tipo, eventId, agregadoId, versionAgregado, payloadJson);
        return eventId;
    }

    protected void publicarEvento(String exchange, String tipo, UUID eventId, UUID agregadoId, long versionAgregado,
                                  String payloadJson) {
        String sobre = """
                {"eventId":"%s","eventType":"%s","version":1,"aggregateId":"%s","aggregateVersion":%d,
                 "occurredAt":"2026-11-20T14:00:00Z","businessDate":"%s","correlationId":"%s","payload":%s}
                """.formatted(eventId, tipo, agregadoId, versionAgregado, DIA, UUID.randomUUID(), payloadJson);
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

    // ----- Fleet falso (R1a y R1b) -----

    protected static String rutaDisponibilidad(UUID busId) {
        return "/fleet/interno/buses/" + busId + "/disponibilidad";
    }

    protected static String rutaChofer(UUID choferId) {
        return "/fleet/interno/choferes/" + choferId;
    }

    /** Respuesta de R1a (DisponibilidadBusRespuesta). */
    protected static String busFleet(UUID busId, UUID sucursalId, String placa, int capacidad, boolean disponible) {
        return """
                {"busId":"%s","sucursalId":"%s","placa":"%s","capacidadPasajeros":%d,"estado":"%s",
                 "disponible":%s,"motivo":%s}
                """.formatted(busId, sucursalId, placa, capacidad, disponible ? "ACTIVO" : "EN_TALLER",
                disponible, disponible ? "null" : "\"BUS_EN_TALLER\"");
    }

    /** Respuesta de R1b (ChoferFleetRespuesta) con salario base 350.00. */
    protected static String choferFleet(UUID choferId, UUID sucursalId, String estado, boolean licenciaVigente) {
        return """
                {"choferId":"%s","sucursalId":"%s","usuarioId":"%s","nombreCompleto":"Juan Perez",
                 "estado":"%s","salarioBaseViaje":350.00,"fechaVencimientoLicencia":"%s","licenciaVigente":%s}
                """.formatted(choferId, sucursalId, UUID.randomUUID(), estado,
                licenciaVigente ? "2028-01-01" : "2026-11-01", licenciaVigente);
    }

    /** Fleet responde que el bus y el chofer están disponibles para un viaje de esa sucursal. */
    protected void fleetDisponible(UUID sucursalId, UUID busId, int capacidad, UUID choferId) {
        FLEET.responder("GET", rutaDisponibilidad(busId), 200, busFleet(busId, sucursalId, "P-111AAA", capacidad, true));
        FLEET.responder("GET", rutaChofer(choferId), 200, choferFleet(choferId, sucursalId, "ACTIVO", true));
    }

    /** Error con el formato de comun, como lo responde otro servicio. */
    protected static String errorRemoto(int status, String codigo, String mensaje) {
        return """
                {"status":%d,"codigo":"%s","mensaje":"%s","ruta":"/remoto","timestamp":"2026-11-20T14:00:00Z",
                 "correlationId":null,"errores":[]}
                """.formatted(status, codigo, mensaje);
    }

    // ----- Datos de prueba directos en la BD -----

    protected UUID insertarSucursal(String nombre, boolean activa) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO vista_sucursales (id, nombre, latitud, longitud, activa, version)
                VALUES (?, ?, 14.834500, -91.518000, ?, 0)""", id, nombre, activa);
        return id;
    }

    protected UUID insertarBus(UUID sucursalId, String placa) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO vista_buses (id, sucursal_id, placa, capacidad_pasajeros, estado, version)
                VALUES (?, ?, ?, 40, 'ACTIVO', 0)""", id, sucursalId, placa);
        return id;
    }

    protected UUID insertarChofer(UUID sucursalId, String nombre) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO vista_choferes (id, sucursal_id, nombre_completo, salario_base_viaje, estado, version)
                VALUES (?, ?, ?, 350.00, 'ACTIVO', 0)""", id, sucursalId, nombre);
        return id;
    }

    protected UUID insertarTarifa(String montoPorKm, LocalDate vigenteDesde) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO vista_tarifas_depreciacion (id, monto_por_km, vigente_desde) VALUES (?, ?, ?)",
                id, new BigDecimal(montoPorKm), vigenteDesde);
        return id;
    }

    protected UUID insertarPrecioAlquiler(String precioPorKm, LocalDate vigenteDesde) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO configuracion_alquiler (id, precio_por_km, vigente_desde, creado_por)
                VALUES (?, ?, ?, ?)""", id, new BigDecimal(precioPorKm), vigenteDesde, UUID.randomUUID());
        return id;
    }

    /** Ruta de 200 km con boleto de 75.00. */
    protected UUID insertarRuta(UUID origenId, UUID destinoId, boolean activa) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO rutas (id, sucursal_origen_id, sucursal_destino_id, distancia_km, precio_boleto, activa,
                                   creado_por)
                VALUES (?, ?, ?, 200.00, 75.00, ?, ?)""", id, origenId, destinoId, activa, UUID.randomUUID());
        return id;
    }

    /** Viaje REGULAR de 4 horas: 40 asientos, boleto 75.00, salario base 350.00, version 0. */
    protected UUID insertarViajeRegular(UUID rutaId, UUID sucursalId, UUID busId, UUID choferId,
                                        OffsetDateTime salida, String estado) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO viajes (id, tipo, ruta_id, sucursal_id, bus_id, chofer_id, capacidad_bus, precio_boleto,
                                    salario_base_chofer, fecha_salida_programada, fecha_llegada_estimada, estado,
                                    creado_por)
                VALUES (?, 'REGULAR', ?, ?, ?, ?, 40, 75.00, 350.00, ?, ?, ?, ?)""",
                id, rutaId, sucursalId, busId, choferId, salida, salida.plusHours(4), estado, UUID.randomUUID());
        return id;
    }

    /** Viaje PRIVADO de 8 horas (sin ruta ni precio de boleto), salario base 350.00, version 0. */
    protected UUID insertarViajePrivado(UUID alquilerId, UUID sucursalId, UUID busId, UUID choferId,
                                        OffsetDateTime salida, String estado) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO viajes (id, tipo, alquiler_id, sucursal_id, bus_id, chofer_id, capacidad_bus,
                                    salario_base_chofer, fecha_salida_programada, fecha_llegada_estimada, estado,
                                    creado_por)
                VALUES (?, 'PRIVADO', ?, ?, ?, ?, 40, 350.00, ?, ?, ?, ?)""",
                id, alquilerId, sucursalId, busId, choferId, salida, salida.plusHours(8), estado, UUID.randomUUID());
        return id;
    }

    /** Salida registrada (T14) de un viaje. */
    protected void insertarSalida(UUID viajeId, UUID busId, UUID choferId, OffsetDateTime fechaHora, String kmInicial) {
        jdbc.update("""
                INSERT INTO salidas_viaje (viaje_id, bus_id, chofer_id, fecha_hora_salida, km_inicial, registrado_por)
                VALUES (?, ?, ?, ?, ?, ?)""",
                viajeId, busId, choferId, fechaHora, new BigDecimal(kmInicial), UUID.randomUUID());
    }

    /**
     * Alquiler EN_LINEA de "Maria Lopez" (Xela → Antigua), 180.50 km, precio estimado 2707.50, version 0.
     * Respeta los CHECK de V11: precio_confirmado (2500.00) salvo en SOLICITADO/CANCELADO; motivo si CANCELADO.
     */
    protected UUID insertarAlquiler(UUID sucursalId, UUID clienteUsuarioId, String estado, int pasajeros,
                                    OffsetDateTime fechaSalida) {
        UUID id = UUID.randomUUID();
        boolean sinPrecio = "SOLICITADO".equals(estado) || "CANCELADO".equals(estado);
        String precioConfirmado = sinPrecio ? "NULL" : "2500.00";
        String motivo = "CANCELADO".equals(estado) ? "'Cambio de planes'" : "NULL";
        jdbc.update("""
                INSERT INTO alquileres_privados (id, sucursal_id, canal, cliente_usuario_id, cliente_nit, cliente_nombre,
                                                 cliente_correo, origen, destino, fecha_salida, cantidad_pasajeros,
                                                 km_estimados, precio_estimado, precio_confirmado, estado,
                                                 motivo_cancelacion, fecha_solicitud, solicitado_por)
                VALUES (?, ?, 'EN_LINEA', ?, 'CF', 'Maria Lopez', 'maria@trips.gt', 'Xela', 'Antigua', ?, ?,
                        180.50, 2707.50, %s, ?, %s, DATE '2026-10-01', ?)""".formatted(precioConfirmado, motivo),
                id, sucursalId, clienteUsuarioId, fechaSalida, pasajeros, estado, clienteUsuarioId);
        return id;
    }

    protected UUID insertarAlquiler(UUID sucursalId, UUID clienteUsuarioId, String estado) {
        return insertarAlquiler(sucursalId, clienteUsuarioId, estado, 30, SALIDA_ALQUILER);
    }

    protected int contar(String tabla) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
        return total == null ? 0 : total;
    }

    protected String estadoDelViaje(UUID viajeId) {
        return jdbc.queryForObject("SELECT estado FROM viajes WHERE id = ?", String.class, viajeId);
    }

    protected String estadoDelAlquiler(UUID alquilerId) {
        return jdbc.queryForObject("SELECT estado FROM alquileres_privados WHERE id = ?", String.class, alquilerId);
    }
}
