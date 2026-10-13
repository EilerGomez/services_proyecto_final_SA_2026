package gt.cunoc.proyecto2.reporting.integracion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import gt.cunoc.proyecto2.comun.seguridad.service.ServicioJwt;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
 * Los 12 reportes (RG1–RG6, RS1–RS6) responden 200 con la BD vacía,
 * los PDF con `application/pdf`, y el scoping por sucursal (403/404).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ReportesIT {

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
    }

    @Autowired
    private Environment entorno;

    @Autowired
    private ServicioJwt jwt;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private RabbitTemplate rabbit;

    private RestClient http;

    private UUID sucursalPropia;
    private UUID sucursalAjena;

    @BeforeEach
    void preparar() {
        http = RestClient.builder()
                .baseUrl("http://localhost:" + entorno.getProperty("local.server.port"))
                .defaultStatusHandler(status -> true, (peticion, respuesta) -> { })
                .build();
        jdbc.execute("TRUNCATE TABLE sucursales, rutas, buses, choferes, viajes, ingresos, gastos, "
                + "tarifas_depreciacion, eventos_procesados CASCADE");
        sucursalPropia = insertarSucursal("Norte");
        sucursalAjena = insertarSucursal("Sur");
    }

    private UUID insertarSucursal(String nombre) {
        UUID id = UUID.randomUUID();
        jdbc.update("INSERT INTO sucursales (id, nombre, latitud, longitud, activa) VALUES (?, ?, ?, ?, ?)",
                id, nombre, new BigDecimal("14.834500"), new BigDecimal("-91.518000"), true);
        return id;
    }

    private String tokenAdminSistema() {
        return jwt.generar(new UsuarioActual(UUID.randomUUID(), "admin@reporting.gt",
                Set.of(Roles.ADMIN_SISTEMA), null, null));
    }

    private String tokenAdminSucursal(UUID sucursalId) {
        return jwt.generar(new UsuarioActual(UUID.randomUUID(), "admin.sucursal@reporting.gt",
                Set.of(Roles.ADMIN_SUCURSAL), sucursalId, null));
    }

    private ResponseEntity<String> getJson(String ruta, String token) {
        String url = ruta.contains("?") ? ruta + "&format=json" : ruta + "?format=json";
        var peticion = http.method(HttpMethod.GET).uri(url);
        if (token != null) {
            peticion.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return peticion.retrieve().toEntity(String.class);
    }

    private ResponseEntity<byte[]> getPdf(String ruta, String token) {
        return http.method(HttpMethod.GET).uri(ruta)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .accept(MediaType.APPLICATION_PDF)
                .retrieve()
                .toEntity(byte[].class);
    }

    // ----- RG1–RG6 (ADMIN_SISTEMA) -----

    @Test
    void reportesGlobalesJsonConBdVaciaResponden200() {
        String token = tokenAdminSistema();
        assertThat(getJson("/reports/ingresos", token).getStatusCode().value()).isEqualTo(200);
        assertThat(getJson("/reports/gastos", token).getStatusCode().value()).isEqualTo(200);
        assertThat(getJson("/reports/ganancias", token).getStatusCode().value()).isEqualTo(200);
        assertThat(getJson("/reports/rutas-demandadas", token).getStatusCode().value()).isEqualTo(200);
        assertThat(getJson("/reports/taller-por-categoria", token).getStatusCode().value()).isEqualTo(200);
        assertThat(getJson("/reports/mapa-rutas", token).getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void reportesGlobalesPdfRespondenPdf() {
        String token = tokenAdminSistema();
        for (String ruta : new String[]{"/reports/ingresos?format=pdf", "/reports/gastos?format=pdf",
                "/reports/ganancias?format=pdf", "/reports/rutas-demandadas?format=pdf",
                "/reports/taller-por-categoria?format=pdf", "/reports/mapa-rutas?format=pdf"}) {
            ResponseEntity<byte[]> respuesta = getPdf(ruta, token);
            assertThat(respuesta.getStatusCode().value()).as(ruta).isEqualTo(200);
            assertThat(respuesta.getHeaders().getContentType()).as(ruta)
                    .isEqualTo(MediaType.APPLICATION_PDF);
            assertThat(respuesta.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).as(ruta)
                    .startsWith("attachment;");
            assertThat(new String(respuesta.getBody(), 0, 5)).as(ruta).isEqualTo("%PDF-");
        }
        // sin format responde PDF por defecto
        ResponseEntity<byte[]> defecto = getPdf("/reports/ganancias", token);
        assertThat(defecto.getStatusCode().value()).isEqualTo(200);
        assertThat(new String(defecto.getBody(), 0, 5)).isEqualTo("%PDF-");
    }

    // ----- RS1–RS6 (scoping por sucursal) -----

    private String[] rutasSucursal(UUID id) {
        String base = "/reports/sucursales/" + id;
        return new String[]{base + "/buses", base + "/choferes", base + "/ingresos-boletos",
                base + "/ingresos-alquiler", base + "/depreciacion", base + "/taller-por-bus"};
    }

    @Test
    void reportesSucursalJsonConBdVaciaResponden200() {
        String sistema = tokenAdminSistema();
        String propia = tokenAdminSucursal(sucursalPropia);
        for (String ruta : rutasSucursal(sucursalPropia)) {
            assertThat(getJson(ruta, sistema).getStatusCode().value()).as(ruta).isEqualTo(200);
            assertThat(getJson(ruta, propia).getStatusCode().value()).as(ruta).isEqualTo(200);
        }
    }

    @Test
    void reportesSucursalPdfRespondenPdf() {
        String token = tokenAdminSistema();
        for (String ruta : rutasSucursal(sucursalPropia)) {
            ResponseEntity<byte[]> respuesta = getPdf(ruta + "?format=pdf", token);
            assertThat(respuesta.getStatusCode().value()).as(ruta).isEqualTo(200);
            assertThat(new String(respuesta.getBody(), 0, 5)).as(ruta).isEqualTo("%PDF-");
        }
    }

    @Test
    void adminSucursalAjenaRecibe403() {
        String ajeno = tokenAdminSucursal(sucursalAjena);
        for (String ruta : rutasSucursal(sucursalPropia)) {
            ResponseEntity<String> respuesta = getJson(ruta, ajeno);
            assertThat(respuesta.getStatusCode().value()).as(ruta).isEqualTo(403);
            assertThat(respuesta.getBody()).contains("SUCURSAL_AJENA");
        }
    }

    @Test
    void sucursalInexistenteRecibe404() {
        String token = tokenAdminSistema();
        ResponseEntity<String> respuesta = getJson(
                "/reports/sucursales/" + UUID.randomUUID() + "/buses", token);
        assertThat(respuesta.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    void sinTokenRecibe401() {
        assertThat(getJson("/reports/ganancias", null).getStatusCode().value()).isEqualTo(401);
        assertThat(getJson("/reports/sucursales/" + sucursalPropia + "/buses", null)
                .getStatusCode().value()).isEqualTo(401);
    }

    // ----- Flujo evento → proyección → reporte (cubre manejadores y proyección) -----

    @Test
    void flujoDeEventosAlimentaLosDoceReportes() {
        UUID sucursalId = sucursalPropia;
        UUID tarifaId = UUID.randomUUID();
        UUID busId = UUID.randomUUID();
        UUID bus2Id = UUID.randomUUID();
        UUID choferId = UUID.randomUUID();
        UUID rutaId = UUID.randomUUID();
        UUID ruta2Id = UUID.randomUUID();
        UUID surId = sucursalAjena;
        UUID viajeId = UUID.randomUUID();
        UUID viaje2Id = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();
        UUID ordenId = UUID.randomUUID();
        UUID servicioId = UUID.randomUUID();
        UUID linea1Id = UUID.randomUUID();
        UUID linea2Id = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();
        UUID usuarioId = UUID.randomUUID();

        publicar("identity.eventos", "TarifaDepreciacionCambiada", tarifaId,
                "{\"tarifaId\":\"" + tarifaId + "\",\"montoPorKm\":1.2500,\"vigenteDesde\":\"2026-10-01\"}");
        publicar("fleet.eventos", "SucursalCreada", sucursalId,
                "{\"sucursalId\":\"" + sucursalId + "\",\"nombre\":\"Norte\",\"latitud\":14.8345,"
                        + "\"longitud\":-91.5180,\"activa\":true}");
        publicar("fleet.eventos", "SucursalActualizada", sucursalId,
                "{\"sucursalId\":\"" + sucursalId + "\",\"nombre\":\"Norte Actualizada\",\"latitud\":14.8345,"
                        + "\"longitud\":-91.5180,\"activa\":true}", 2L);
        publicar("fleet.eventos", "BusCreado", busId,
                busJson(busId, sucursalId, "C123ABC", "ACTIVO"));
        publicar("fleet.eventos", "BusActualizado", busId,
                busJson(busId, sucursalId, "C123ABC", "EN_RUTA"), 2L);
        publicar("fleet.eventos", "BusCreado", bus2Id,
                busJson(bus2Id, sucursalId, "C999ZZZ", "ACTIVO"));
        publicar("fleet.eventos", "ChoferCreado", choferId,
                "{\"choferId\":\"" + choferId + "\",\"usuarioId\":\"" + usuarioId + "\","
                        + "\"sucursalId\":\"" + sucursalId + "\",\"nombreCompleto\":\"Pedro Ramírez\","
                        + "\"numeroLicencia\":\"LIC-889900\",\"tipoLicencia\":\"A\","
                        + "\"fechaVencimientoLicencia\":\"2027-03-15\",\"estado\":\"ACTIVO\"}");
        publicar("fleet.eventos", "ChoferActualizado", choferId,
                "{\"choferId\":\"" + choferId + "\",\"usuarioId\":\"" + usuarioId + "\","
                        + "\"sucursalId\":\"" + sucursalId + "\",\"nombreCompleto\":\"Pedro Ramírez\","
                        + "\"numeroLicencia\":\"LIC-889900\",\"tipoLicencia\":\"A\","
                        + "\"fechaVencimientoLicencia\":\"2027-03-15\",\"estado\":\"ACTIVO\"}", 2L);
        publicar("trips.eventos", "RutaCreada", rutaId,
                rutaJson(rutaId, sucursalId, surId, "Norte", "Sur"));
        publicar("trips.eventos", "RutaActualizada", rutaId,
                rutaJson(rutaId, sucursalId, surId, "Norte", "Sur"));
        publicar("trips.eventos", "RutaCreada", ruta2Id,
                rutaJson(ruta2Id, sucursalId, surId, "Norte", "Sur"));
        publicar("trips.eventos", "ViajeProgramado", viajeId,
                viajeJson(viajeId, rutaId, sucursalId, busId, choferId));
        publicar("trips.eventos", "ViajeActualizado", viajeId,
                viajeJson(viajeId, rutaId, sucursalId, busId, choferId), 2L);
        publicar("trips.eventos", "ViajeProgramado", viaje2Id,
                viajeJson(viaje2Id, ruta2Id, sucursalId, bus2Id, choferId));
        publicar("trips.eventos", "ViajeIniciado", viajeId,
                "{\"viajeId\":\"" + viajeId + "\",\"busId\":\"" + busId + "\",\"choferId\":\""
                        + choferId + "\"}", 3L);
        publicar("trips.eventos", "ViajeCompletado", viajeId,
                "{\"viajeId\":\"" + viajeId + "\",\"tipo\":\"REGULAR\",\"rutaId\":\"" + rutaId + "\","
                        + "\"sucursalId\":\"" + sucursalId + "\",\"busId\":\"" + busId + "\","
                        + "\"choferId\":\"" + choferId + "\",\"kmRecorridos\":207.50,"
                        + "\"costoCombustible\":400.00,\"tarifaDepreciacion\":1.2500,"
                        + "\"costoDepreciacion\":259.38,\"salarioChofer\":350.00,"
                        + "\"fechaHoraLlegada\":\"2026-10-20T18:00:00Z\"}", 4L);
        publicar("sales.eventos", "BoletoVendido", ventaId,
                "{\"ventaId\":\"" + ventaId + "\",\"viajeId\":\"" + viajeId + "\",\"rutaId\":\""
                        + rutaId + "\",\"busId\":\"" + busId + "\",\"sucursalId\":\"" + sucursalId + "\","
                        + "\"clienteNit\":\"1234567-8\",\"clienteNombre\":\"Ana López\","
                        + "\"cantidadBoletos\":2,\"montoTotal\":150.00,"
                        + "\"origenNombre\":\"Norte\",\"destinoNombre\":\"Sur\"}");
        publicar("sales.eventos", "VentaCancelada", ventaId,
                "{\"ventaId\":\"" + ventaId + "\",\"viajeId\":\"" + viajeId + "\","
                        + "\"sucursalId\":\"" + sucursalId + "\",\"clienteNit\":\"1234567-8\","
                        + "\"clienteNombre\":\"Ana López\",\"cantidadBoletos\":2,\"montoAnulado\":150.00}");
        publicar("sales.eventos", "AlquilerPagado", ordenId,
                "{\"ordenId\":\"" + ordenId + "\",\"sucursalId\":\"" + sucursalId + "\","
                        + "\"clienteNit\":\"1234567-8\",\"clienteNombre\":\"Ana López\","
                        + "\"origen\":\"Norte, parque central\",\"destino\":\"Antigua Guatemala\","
                        + "\"monto\":4300.00}");
        publicar("maintenance.eventos", "GastoTallerRegistrado", servicioId,
                "{\"servicioId\":\"" + servicioId + "\",\"busId\":\"" + busId + "\","
                        + "\"sucursalId\":\"" + sucursalId + "\",\"fechaServicio\":\"2026-10-20\",\"lineas\":["
                        + "{\"lineaId\":\"" + linea1Id + "\",\"categoriaId\":\"" + categoriaId + "\","
                        + "\"categoriaNombre\":\"Llantas\",\"descripcion\":\"Cambio\",\"tipo\":\"REPUESTO\","
                        + "\"monto\":6800.00},"
                        + "{\"lineaId\":\"" + linea2Id + "\",\"categoriaId\":\"" + categoriaId + "\","
                        + "\"categoriaNombre\":\"Llantas\",\"descripcion\":\"Mano de obra\",\"tipo\":\"MANO_OBRA\","
                        + "\"monto\":400.00}]}");
        publicar("maintenance.eventos", "CategoriaGastoActualizada", categoriaId,
                "{\"categoriaId\":\"" + categoriaId + "\",\"nombre\":\"Llantas Plus\",\"activa\":true}");

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(contar("ingresos")).isEqualTo(3);
            assertThat(contar("gastos")).isEqualTo(5);
        });
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(jdbc.queryForObject(
                        "SELECT estado FROM viajes WHERE viaje_id = ?", String.class, viajeId))
                        .isEqualTo("COMPLETADO"));

        String sistema = tokenAdminSistema();
        String propia = tokenAdminSucursal(sucursalPropia);

        // RG1: 150 - 150 + 4300
        assertThat(getJson("/reports/ingresos", sistema).getBody()).contains("4300.0");
        // RG2: combustible 400 + depreciación 259.38 + salario 350 + taller 400 + repuestos 6800
        String gastos = getJson("/reports/gastos", sistema).getBody();
        assertThat(gastos).contains("\"taller\":400.00");
        assertThat(gastos).contains("\"repuestos\":6800.00");
        // RG3: hay filas con ganancia
        assertThat(getJson("/reports/ganancias", sistema).getBody()).contains("Norte Actualizada");
        // RG4: la ruta suma boletos 2 - 2 = 0 pero conserva ingresos 0
        assertThat(getJson("/reports/rutas-demandadas", sistema).getStatusCode().value()).isEqualTo(200);
        // RG5: categoría renombrada por CategoriaGastoActualizada
        assertThat(getJson("/reports/taller-por-categoria", sistema).getBody())
                .contains("Llantas Plus");
        // RG6: sucursales + rutas (ruta2 eliminada)
        assertThat(getJson("/reports/mapa-rutas", sistema).getBody()).contains("Norte Actualizada");
        // RS1: buses con chofer actual resuelto
        assertThat(getJson("/reports/sucursales/" + sucursalId + "/buses", propia).getBody())
                .contains("C123ABC");
        // RS2: chofer con sus viajes contados
        assertThat(getJson("/reports/sucursales/" + sucursalId + "/choferes", propia).getBody())
                .contains("Pedro Ramírez");
        // RS3/RS4
        assertThat(getJson("/reports/sucursales/" + sucursalId + "/ingresos-boletos", propia).getBody())
                .contains("150.0");
        assertThat(getJson("/reports/sucursales/" + sucursalId + "/ingresos-alquiler", propia).getBody())
                .contains("4300.0");
        // RS5: depreciación del viaje completado + tarifa vigente
        assertThat(getJson("/reports/sucursales/" + sucursalId + "/depreciacion", propia).getBody())
                .contains("259.38");
        // RS6: taller del bus por categoría
        assertThat(getJson("/reports/sucursales/" + sucursalId + "/taller-por-bus", propia).getBody())
                .contains("6800.0");

        // Destructivos sobre entidades secundarias (ruta2/viaje2/bus2), con la proyección ya verificada
        publicar("fleet.eventos", "BusDesactivado", bus2Id,
                "{\"busId\":\"" + bus2Id + "\"}", 2L);
        publicar("fleet.eventos", "ChoferDesactivado", choferId,
                "{\"choferId\":\"" + choferId + "\"}", 3L);
        publicar("trips.eventos", "RutaEliminada", ruta2Id,
                "{\"rutaId\":\"" + ruta2Id + "\"}");
        publicar("trips.eventos", "ViajeEliminado", viaje2Id,
                "{\"viajeId\":\"" + viaje2Id + "\"}", 2L);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(jdbc.queryForObject(
                    "SELECT estado FROM buses WHERE id = ?", String.class, bus2Id))
                    .isEqualTo("INACTIVO");
            assertThat(jdbc.queryForObject(
                    "SELECT estado FROM choferes WHERE id = ?", String.class, choferId))
                    .isEqualTo("INACTIVO");
            assertThat(contar("rutas WHERE id = '" + ruta2Id + "'")).isZero();
            assertThat(contar("viajes WHERE viaje_id = '" + viaje2Id + "'")).isZero();
        });
        // Destructivos aplicados
        assertThat(getJson("/reports/sucursales/" + sucursalId + "/choferes", propia).getBody())
                .contains("INACTIVO");
    }

    // ----- version = aggregateVersion de Fleet/Trips (los cambios locales no la suben) -----

    @Test
    void busYChoferConViajesCompletadosSiguenAceptandoEventosDeFleet() {
        UUID sucursalId = sucursalPropia;
        UUID busId = UUID.randomUUID();
        UUID choferId = UUID.randomUUID();
        UUID rutaId = UUID.randomUUID();

        publicar("fleet.eventos", "BusCreado", busId,
                busJson(busId, sucursalId, "C123ABC", "ACTIVO"), 1L);
        publicar("fleet.eventos", "ChoferCreado", choferId, choferJson(choferId, sucursalId), 1L);
        publicar("trips.eventos", "RutaCreada", rutaId,
                rutaJson(rutaId, sucursalId, sucursalAjena, "Norte", "Sur"));
        // Tres viajes completos: suben total_viajes y cambian chofer_actual_id (cambios locales)
        for (int i = 0; i < 3; i++) {
            UUID viajeId = UUID.randomUUID();
            publicar("trips.eventos", "ViajeProgramado", viajeId,
                    viajeJson(viajeId, rutaId, sucursalId, busId, choferId), 1L);
            publicar("trips.eventos", "ViajeIniciado", viajeId,
                    "{\"viajeId\":\"" + viajeId + "\",\"busId\":\"" + busId + "\",\"choferId\":\""
                            + choferId + "\"}", 2L);
            publicar("trips.eventos", "ViajeCompletado", viajeId,
                    "{\"viajeId\":\"" + viajeId + "\",\"tipo\":\"REGULAR\",\"rutaId\":\"" + rutaId + "\","
                            + "\"sucursalId\":\"" + sucursalId + "\",\"busId\":\"" + busId + "\","
                            + "\"choferId\":\"" + choferId + "\",\"kmRecorridos\":200.00,"
                            + "\"costoCombustible\":400.00,\"tarifaDepreciacion\":1.2500,"
                            + "\"costoDepreciacion\":250.00,\"salarioChofer\":350.00}", 3L);
        }
        // contar(...) no lanza excepción si la fila todavía no existe
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(contar("buses WHERE id = '" + busId + "' AND total_viajes = 3")).isEqualTo(1);
            assertThat(contar("choferes WHERE id = '" + choferId + "' AND total_viajes = 3")).isEqualTo(1);
        });
        // Los cambios locales no tocaron la versión de Fleet
        assertThat(jdbc.queryForObject("SELECT version FROM buses WHERE id = ?", Integer.class, busId))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT version FROM choferes WHERE id = ?", Integer.class, choferId))
                .isEqualTo(1);

        // Siguiente versión de Fleet: se aplica (antes se descartaba sin error)
        publicar("fleet.eventos", "BusActualizado", busId,
                busJson(busId, sucursalId, "C123ABC", "EN_TALLER"), 2L);
        publicar("fleet.eventos", "ChoferDesactivado", choferId,
                "{\"choferId\":\"" + choferId + "\"}", 2L);

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() -> {
            assertThat(jdbc.queryForMap("SELECT estado, version, total_viajes FROM buses WHERE id = ?", busId))
                    .containsEntry("estado", "EN_TALLER")
                    .containsEntry("version", 2)
                    .containsEntry("total_viajes", 3);
            assertThat(jdbc.queryForMap("SELECT estado, version, total_viajes FROM choferes WHERE id = ?",
                    choferId))
                    .containsEntry("estado", "INACTIVO")
                    .containsEntry("version", 2)
                    .containsEntry("total_viajes", 3);
        });
    }

    // ----- cliente_nombre de hasta 201 caracteres (V3) -----

    @Test
    void clienteNombreDe201CaracteresSeGuarda() {
        UUID ordenId = UUID.randomUUID();
        String nombre = "N".repeat(100) + " " + "A".repeat(100);
        publicar("sales.eventos", "AlquilerPagado", ordenId,
                "{\"ordenId\":\"" + ordenId + "\",\"sucursalId\":\"" + sucursalPropia + "\","
                        + "\"clienteNit\":\"CF\",\"clienteNombre\":\"" + nombre + "\","
                        + "\"origen\":\"Norte\",\"destino\":\"Sur\",\"monto\":100.00}");

        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(contar("ingresos WHERE referencia_id = '" + ordenId + "'")).isEqualTo(1));
        assertThat(jdbc.queryForObject(
                "SELECT cliente_nombre FROM ingresos WHERE referencia_id = ?", String.class, ordenId))
                .hasSize(201)
                .isEqualTo(nombre);
    }

    private String busJson(UUID busId, UUID sucursalId, String placa, String estado) {
        return "{\"busId\":\"" + busId + "\",\"sucursalId\":\"" + sucursalId + "\",\"placa\":\"" + placa
                + "\",\"marca\":\"Volvo\",\"modelo\":\"B12\",\"capacidadPasajeros\":50,\"estado\":\"" + estado
                + "\",\"kilometrajeActual\":1000.00}";
    }

    private String choferJson(UUID choferId, UUID sucursalId) {
        return "{\"choferId\":\"" + choferId + "\",\"usuarioId\":\"" + UUID.randomUUID() + "\","
                + "\"sucursalId\":\"" + sucursalId + "\",\"nombreCompleto\":\"Luis Pérez\","
                + "\"numeroLicencia\":\"LIC-112233\",\"tipoLicencia\":\"A\","
                + "\"fechaVencimientoLicencia\":\"2027-03-15\",\"estado\":\"ACTIVO\"}";
    }

    private String rutaJson(UUID rutaId, UUID origenId, UUID destinoId, String origen, String destino) {
        return "{\"rutaId\":\"" + rutaId + "\",\"sucursalOrigenId\":\"" + origenId + "\","
                + "\"sucursalDestinoId\":\"" + destinoId + "\",\"origenNombre\":\"" + origen + "\","
                + "\"destinoNombre\":\"" + destino + "\",\"distanciaKm\":205.00,\"precioBoleto\":75.00,"
                + "\"activa\":true}";
    }

    private String viajeJson(UUID viajeId, UUID rutaId, UUID sucursalId, UUID busId, UUID choferId) {
        return "{\"viajeId\":\"" + viajeId + "\",\"tipo\":\"REGULAR\",\"rutaId\":\"" + rutaId + "\","
                + "\"sucursalId\":\"" + sucursalId + "\",\"busId\":\"" + busId + "\",\"choferId\":\""
                + choferId + "\",\"fechaSalida\":\"2026-10-20T08:00:00Z\"}";
    }

    /** Publica un evento como lo haría el relay del servicio dueño (sobre completo de comun). */
    private void publicar(String exchange, String tipo, UUID agregadoId, String payloadJson) {
        publicar(exchange, tipo, agregadoId, payloadJson, 1L);
    }

    private void publicar(String exchange, String tipo, UUID agregadoId, String payloadJson,
                          Long versionAgregado) {
        UUID eventId = UUID.randomUUID();
        String sobre = "{\"eventId\":\"" + eventId + "\",\"eventType\":\"" + tipo + "\",\"version\":1,"
                + "\"aggregateId\":\"" + agregadoId + "\",\"aggregateVersion\":" + versionAgregado + ","
                + "\"occurredAt\":\"2026-10-20T14:00:00Z\",\"businessDate\":\"2026-10-20\","
                + "\"correlationId\":\"" + UUID.randomUUID() + "\",\"payload\":" + payloadJson + "}";
        rabbit.send(exchange, tipo, new org.springframework.amqp.core.Message(
                sobre.getBytes(StandardCharsets.UTF_8),
                new org.springframework.amqp.core.MessageProperties()));
    }

    private int contar(String tabla) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM " + tabla, Integer.class);
        return total == null ? 0 : total;
    }
}
