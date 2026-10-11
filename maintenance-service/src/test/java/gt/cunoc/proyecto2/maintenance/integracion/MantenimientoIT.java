package gt.cunoc.proyecto2.maintenance.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import gt.cunoc.proyecto2.comun.seguridad.service.ServicioJwt;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.rabbitmq.RabbitMQContainer;

/**
 * M1–M7 contra PostgreSQL y RabbitMQ reales, con las 8 categorías precargadas.
 * Verifica el flujo en ambos sentidos: consume BusCreado de Fleet y publica
 * GastoTallerRegistrado / CategoriaGastoActualizada en maintenance.eventos.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class MantenimientoIT {

    private static final String ESPIA = "pruebas.maintenance.eventos";

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
    }

    @Autowired
    private Environment entorno;
    @Autowired
    private ServicioJwt jwt;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private RabbitTemplate rabbit;
    @Autowired
    private AmqpAdmin amqp;

    private RestClient http;

    @BeforeEach
    void preparar() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + entorno.getProperty("local.server.port"))
                .defaultStatusHandler(status -> true, (peticion, respuesta) -> { })
                .build();
        jdbc.execute("TRUNCATE TABLE detalle_servicio_taller, servicios_taller, vista_buses, "
                + "eventos_salida, eventos_procesados CASCADE");
        Queue espia = new Queue(ESPIA, true, false, false);
        amqp.declareQueue(espia);
        amqp.declareBinding(BindingBuilder.bind(espia)
                .to(new TopicExchange("maintenance.eventos")).with("#"));
        amqp.purgeQueue(ESPIA, false);
    }

    private String tokenAdminSistema() {
        return jwt.generar(new UsuarioActual(UUID.randomUUID(), "admin@m.gt",
                Set.of(Roles.ADMIN_SISTEMA), null, null));
    }

    private String tokenAdminSucursal(UUID sucursalId) {
        return jwt.generar(new UsuarioActual(UUID.randomUUID(), "suc@m.gt",
                Set.of(Roles.ADMIN_SUCURSAL), sucursalId, null));
    }

    private ResponseEntity<String> get(String ruta, String token) {
        var peticion = http.method(HttpMethod.GET).uri(ruta);
        if (token != null) {
            peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return peticion.retrieve().toEntity(String.class);
    }

    private ResponseEntity<String> post(String ruta, String token, String json) {
        return http.method(HttpMethod.POST).uri(ruta)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json)
                .retrieve().toEntity(String.class);
    }

    private ResponseEntity<String> put(String ruta, String token, String json) {
        return http.method(HttpMethod.PUT).uri(ruta)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(json)
                .retrieve().toEntity(String.class);
    }

    /** Publica un evento de Fleet como lo haría su relay (sobre completo de comun). */
    private void publicarBus(String tipo, UUID busId, UUID sucursalId, String placa, String estado,
                             Long version) {
        String payload = "{\"busId\":\"" + busId + "\",\"sucursalId\":\"" + sucursalId + "\","
                + "\"placa\":\"" + placa + "\",\"marca\":\"Volvo\",\"modelo\":\"B12\","
                + "\"anioFabricacion\":2018,\"capacidadPasajeros\":50,\"estado\":\"" + estado + "\","
                + "\"kilometrajeActual\":1000.00}";
        String sobre = "{\"eventId\":\"" + UUID.randomUUID() + "\",\"eventType\":\"" + tipo + "\","
                + "\"version\":1,\"aggregateId\":\"" + busId + "\",\"aggregateVersion\":" + version + ","
                + "\"occurredAt\":\"2026-10-20T14:00:00Z\",\"businessDate\":\"2026-10-20\","
                + "\"correlationId\":\"" + UUID.randomUUID() + "\",\"payload\":" + payload + "}";
        rabbit.send("fleet.eventos", tipo, MessageBuilder.withBody(
                sobre.getBytes(StandardCharsets.UTF_8))
                .setContentType(MediaType.APPLICATION_JSON_VALUE)
                .build());
    }

    /** Espera (máx. 15 s) los eventos publicados en maintenance.eventos. */
    private Map<String, String> esperarEventos(String... tipos) {
        Map<String, String> recibidos = new HashMap<>();
        long limite = System.currentTimeMillis() + 15_000;
        while (!recibidos.keySet().containsAll(java.util.List.of(tipos))
                && System.currentTimeMillis() < limite) {
            Message mensaje = rabbit.receive(ESPIA, 500);
            if (mensaje != null) {
                String sobre = new String(mensaje.getBody(), StandardCharsets.UTF_8);
                String compacto = sobre.replace(" ", "");
                for (String tipo : tipos) {
                    if (compacto.contains("\"eventType\":\"" + tipo + "\"")) {
                        recibidos.put(tipo, sobre);
                    }
                }
            }
        }
        assertThat(recibidos).containsKeys(tipos);
        return recibidos;
    }

    private void esperarBusEnVista(UUID busId) {
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(jdbc.queryForObject("SELECT count(*) FROM vista_buses WHERE bus_id = ?",
                        Integer.class, busId)).isEqualTo(1));
    }

    private UUID busDePrueba(UUID sucursalId, String placa) {
        UUID busId = UUID.randomUUID();
        publicarBus("BusCreado", busId, sucursalId, placa, "ACTIVO", 0L);
        esperarBusEnVista(busId);
        return busId;
    }

    private String categoriaIdPorNombre(String cuerpo, String nombre) {
        // M1 devuelve arreglo: busca el objeto con ese nombre y extrae su categoriaId
        int inicio = cuerpo.indexOf("\"nombre\":\"" + nombre + "\"");
        assertThat(inicio).isGreaterThanOrEqualTo(0);
        int id = cuerpo.lastIndexOf("\"categoriaId\":\"", inicio);
        return cuerpo.substring(id + 15, cuerpo.indexOf('"', id + 15));
    }

    @Test
    void m1ListaLasOchoCategoriasPrecargadas() {
        ResponseEntity<String> respuesta = get("/maintenance/categorias", tokenAdminSistema());

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(respuesta.getBody()).contains("Llantas", "Cambio de aceite");
    }

    @Test
    void m2CreaCategoriaYPublicaEvento() {
        String nombre = "Cat-" + UUID.randomUUID().toString().substring(0, 8);
        ResponseEntity<String> respuesta = post("/maintenance/categorias", tokenAdminSistema(),
                "{\"nombre\":\"" + nombre + "\",\"descripcion\":\"Prueba\"}");

        assertThat(respuesta.getStatusCode().value()).isEqualTo(201);
        assertThat(respuesta.getBody()).contains(nombre);
        Map<String, String> eventos = esperarEventos("CategoriaGastoActualizada");
        assertThat(eventos.get("CategoriaGastoActualizada")).contains(nombre);
    }

    @Test
    void m2DuplicadaEs409() {
        ResponseEntity<String> respuesta = post("/maintenance/categorias", tokenAdminSistema(),
                "{\"nombre\":\"Motor\",\"descripcion\":null}");

        assertThat(respuesta.getStatusCode().value()).isEqualTo(409);
        assertThat(respuesta.getBody()).contains("CATEGORIA_DUPLICADA");
    }

    @Test
    void m3ActualizaYPublicaEvento() {
        String sistema = tokenAdminSistema();
        String lista = get("/maintenance/categorias", sistema).getBody();
        String id = categoriaIdPorNombre(lista, "Frenos");

        ResponseEntity<String> respuesta = put("/maintenance/categorias/" + id, sistema,
                "{\"nombre\":\"Frenos Plus\",\"descripcion\":null,\"activa\":true}");

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(respuesta.getBody()).contains("Frenos Plus");
        assertThat(esperarEventos("CategoriaGastoActualizada")
                .get("CategoriaGastoActualizada")).contains("Frenos Plus");
    }

    @Test
    void m4ListaBusesDeLaSucursalTrasBusCreado() {
        UUID sucursalId = UUID.randomUUID();
        UUID busId = busDePrueba(sucursalId, "C123ABC");

        ResponseEntity<String> respuesta =
                get("/maintenance/buses", tokenAdminSucursal(sucursalId));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(respuesta.getBody()).contains(busId.toString()).contains("C123ABC");
        assertThat(get("/maintenance/buses?activo=false", tokenAdminSucursal(sucursalId)).getBody())
                .doesNotContain("C123ABC");
    }

    @Test
    void m5RegistraServicioConTotalesDelTriggerYPublicaEvento() {
        UUID sucursalId = UUID.randomUUID();
        String sucursal = tokenAdminSucursal(sucursalId);
        UUID busId = busDePrueba(sucursalId, "C123ABC");
        String categorias = get("/maintenance/categorias", tokenAdminSistema()).getBody();
        String llantas = categoriaIdPorNombre(categorias, "Llantas");

        ResponseEntity<String> respuesta = post("/maintenance/servicios", sucursal,
                "{\"busId\":\"" + busId + "\",\"fechaServicio\":\"2026-10-22\","
                        + "\"taller\":\"Taller Hermanos Pérez\",\"observaciones\":\"150 000 km\","
                        + "\"lineas\":[{\"categoriaId\":\"" + llantas + "\","
                        + "\"descripcion\":\"4 llantas\",\"tipo\":\"REPUESTO\",\"monto\":6800.00},"
                        + "{\"categoriaId\":\"" + llantas + "\","
                        + "\"descripcion\":\"Montaje\",\"tipo\":\"MANO_OBRA\",\"monto\":400.00}]}");

        assertThat(respuesta.getStatusCode().value()).isEqualTo(201);
        assertThat(respuesta.getBody()).contains("\"totalManoObra\":400.00");
        assertThat(respuesta.getBody()).contains("\"totalRepuestos\":6800.00");
        assertThat(respuesta.getBody()).contains("\"total\":7200.00");
        Map<String, String> eventos = esperarEventos("GastoTallerRegistrado");
        assertThat(eventos.get("GastoTallerRegistrado")).contains("6800.00").contains("Llantas");
    }

    @Test
    void m5BusInexistenteEs404BusInactivoEs422YOtraSucursalEs403() {
        UUID sucursalId = UUID.randomUUID();
        String sucursal = tokenAdminSucursal(sucursalId);
        String otro = tokenAdminSucursal(UUID.randomUUID());
        String categorias = get("/maintenance/categorias", tokenAdminSistema()).getBody();
        String llantas = categoriaIdPorNombre(categorias, "Llantas");
        String linea = "\"lineas\":[{\"categoriaId\":\"" + llantas + "\",\"descripcion\":\"X\","
                + "\"tipo\":\"REPUESTO\",\"monto\":10.00}]";

        ResponseEntity<String> noExiste = post("/maintenance/servicios", sucursal,
                "{\"busId\":\"" + UUID.randomUUID() + "\",\"fechaServicio\":\"2026-10-22\"," + linea + "}");
        assertThat(noExiste.getStatusCode().value()).isEqualTo(404);
        assertThat(noExiste.getBody()).contains("BUS_NO_EXISTE");

        UUID busId = busDePrueba(sucursalId, "C999ZZZ");
        ResponseEntity<String> ajeno = post("/maintenance/servicios", otro,
                "{\"busId\":\"" + busId + "\",\"fechaServicio\":\"2026-10-22\"," + linea + "}");
        assertThat(ajeno.getStatusCode().value()).isEqualTo(403);

        publicarBus("BusDesactivado", busId, sucursalId, "C999ZZZ", "INACTIVO", 1L);
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(jdbc.queryForObject("SELECT activo FROM vista_buses WHERE bus_id = ?",
                        Boolean.class, busId)).isFalse());
        ResponseEntity<String> inactivo = post("/maintenance/servicios", sucursal,
                "{\"busId\":\"" + busId + "\",\"fechaServicio\":\"2026-10-22\"," + linea + "}");
        assertThat(inactivo.getStatusCode().value()).isEqualTo(422);
        assertThat(inactivo.getBody()).contains("BUS_INACTIVO");
    }

    @Test
    void m6ListaYM7Detalla() {
        UUID sucursalId = UUID.randomUUID();
        String sucursal = tokenAdminSucursal(sucursalId);
        UUID busId = busDePrueba(sucursalId, "C123ABC");
        String categorias = get("/maintenance/categorias", tokenAdminSistema()).getBody();
        String llantas = categoriaIdPorNombre(categorias, "Llantas");
        String creado = post("/maintenance/servicios", sucursal,
                "{\"busId\":\"" + busId + "\",\"fechaServicio\":\"2026-10-22\"," + "\"lineas\":["
                        + "{\"categoriaId\":\"" + llantas + "\",\"descripcion\":\"Aceite\","
                        + "\"tipo\":\"MANO_OBRA\",\"monto\":350.00}]}").getBody();
        String servicioId = creado.substring(creado.indexOf("\"servicioId\":\"") + 14,
                creado.indexOf('"', creado.indexOf("\"servicioId\":\"") + 14));

        ResponseEntity<String> lista = get("/maintenance/servicios", sucursal);
        assertThat(lista.getStatusCode().value()).isEqualTo(200);
        assertThat(lista.getBody()).contains(servicioId).doesNotContain("lineas");
        assertThat(get("/maintenance/servicios?categoriaId=" + llantas, sucursal).getBody())
                .contains(servicioId);

        ResponseEntity<String> detalle = get("/maintenance/servicios/" + servicioId, sucursal);
        assertThat(detalle.getStatusCode().value()).isEqualTo(200);
        assertThat(detalle.getBody()).contains("Aceite").contains("Llantas");

        assertThat(get("/maintenance/servicios/" + UUID.randomUUID(), sucursal)
                .getStatusCode().value()).isEqualTo(404);
        assertThat(get("/maintenance/servicios/" + servicioId, tokenAdminSucursal(UUID.randomUUID()))
                .getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void sinTokenEs401() {
        assertThat(get("/maintenance/categorias", null).getStatusCode().value()).isEqualTo(401);
    }
}
