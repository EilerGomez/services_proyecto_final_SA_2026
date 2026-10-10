package gt.cunoc.proyecto2.reporting.integracion;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.comun.seguridad.service.ServicioJwt;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
        var peticion = http.method(HttpMethod.GET).uri(ruta);
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
        for (String ruta : new String[]{"/reports/ingresos/pdf", "/reports/gastos/pdf",
                "/reports/ganancias/pdf", "/reports/rutas-demandadas/pdf",
                "/reports/taller-por-categoria/pdf", "/reports/mapa-rutas/pdf"}) {
            ResponseEntity<byte[]> respuesta = getPdf(ruta, token);
            assertThat(respuesta.getStatusCode().value()).as(ruta).isEqualTo(200);
            assertThat(respuesta.getHeaders().getContentType()).as(ruta)
                    .isEqualTo(MediaType.APPLICATION_PDF);
            assertThat(respuesta.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).as(ruta)
                    .startsWith("attachment;");
            assertThat(new String(respuesta.getBody(), 0, 5)).as(ruta).isEqualTo("%PDF-");
        }
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
            ResponseEntity<byte[]> respuesta = getPdf(ruta + "/pdf", token);
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
}
