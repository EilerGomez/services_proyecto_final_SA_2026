package gt.cunoc.proyecto2.fleet.integracion.choferes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import gt.cunoc.proyecto2.fleet.integracion.soporte.PruebaIntegracion;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Reglas del alta y la baja de choferes que no se ven en el camino feliz: el reintento del alta (el
 * frontend perdió la respuesta y vuelve a mandar el mismo solicitudId), el solicitudId reusado para
 * otro registro, la sucursal desactivada, la licencia de otro chofer y la baja repetida.
 */
class ReglasChoferIT extends PruebaIntegracion {

    private static final Duration ESPERA = Duration.ofSeconds(10);
    private static final String RUTA_R5 = "/identity/interno/cuentas-personal";
    private static final String RUTA_R3 = "/trips/interno/viajes/activos";
    private static final LocalDate VENCE = LocalDate.of(2028, 6, 30);

    private UUID xela;

    @BeforeEach
    void datos() {
        xela = insertarSucursal("XELA", true);
    }

    private static String nuevoChofer(UUID sucursalId, UUID solicitudId, String licencia) {
        return """
                {"sucursalId":"%s","numeroLicencia":"%s","tipoLicencia":"A",
                 "fechaVencimientoLicencia":"2028-06-30","salarioBaseViaje":350.00,"fechaRegistro":"2026-10-20",
                 "cuenta":{"solicitudId":"%s","correo":"juan@fleet.gt","nombres":"Juan","apellidos":"Perez",
                           "nit":"CF","dpi":"1234567890101","telefono":"55551234","direccion":"Zona 1",
                           "fotoUrl":"%s"}}
                """.formatted(sucursalId, licencia, solicitudId, FOTO_CHOFER);
    }

    /** Alta completa de un chofer nuevo con la cuenta que crea Identity (R5). */
    private UUID crearChofer(UUID solicitudId, String licencia) {
        IDENTITY.responder("POST", RUTA_R5, 201, cuentaCreada(UUID.randomUUID(), "juan@fleet.gt", "Juan Perez"));
        ResponseEntity<String> respuesta = post("/fleet/choferes", tokenAdminSucursal(xela),
                nuevoChofer(xela, solicitudId, licencia));
        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(201);
        return solicitudId;
    }

    // ----- F15: alta -----

    @Test
    void elReintentoDelAltaDevuelveElMismoChoferSinVolverALlamarAIdentity() {
        UUID solicitudId = UUID.randomUUID();
        crearChofer(solicitudId, "lic-2024-77");

        // El mismo solicitudId con los mismos datos: el alta ya se guardó, se devuelve tal cual
        ResponseEntity<String> reintento = post("/fleet/choferes", tokenAdminSucursal(xela),
                nuevoChofer(xela, solicitudId, "lic-2024-77"));

        assertThat(reintento.getStatusCode().value()).as(reintento.getBody()).isEqualTo(201);
        assertThat(PruebaIntegracion.<String>json(reintento, "$.choferId")).isEqualTo(solicitudId.toString());
        assertThat(IDENTITY.peticiones()).hasSize(1);        // la cuenta no se creó dos veces
        assertThat(contar("choferes")).isEqualTo(1);
    }

    @Test
    void unSolicitudIdReusadoParaOtraLicenciaOOtraSucursalSeRechaza() {
        UUID solicitudId = UUID.randomUUID();
        crearChofer(solicitudId, "lic-2024-77");
        UUID guate = insertarSucursal("GUATE", true);

        // Mismo solicitudId pero otros datos: no es un reintento, es un id reusado
        esError(post("/fleet/choferes", tokenAdminSucursal(xela), nuevoChofer(xela, solicitudId, "lic-otra-99")),
                409, "SOLICITUD_YA_USADA");
        // El alta de choferes es del admin de sucursal: el de Guate pide el alta en la suya
        esError(post("/fleet/choferes", tokenAdminSucursal(guate), nuevoChofer(guate, solicitudId, "lic-2024-77")),
                409, "SOLICITUD_YA_USADA");
        assertThat(contar("choferes")).isEqualTo(1);
    }

    @Test
    void enUnaSucursalDesactivadaNoSeRegistranChoferes() {
        UUID cerrada = insertarSucursal("CERRADA", false);

        esError(post("/fleet/choferes", tokenAdminSucursal(cerrada),
                nuevoChofer(cerrada, UUID.randomUUID(), "lic-2024-77")), 422, "SUCURSAL_INACTIVA");
        assertThat(IDENTITY.peticiones()).isEmpty();
        assertThat(contar("choferes")).isZero();
    }

    // ----- F17: licencia -----

    @Test
    void alRenovarNoSeLePuedePonerLaLicenciaDeOtroChofer() {
        UUID chofer = insertarChofer(xela, UUID.randomUUID(), "LIC-1", VENCE);
        insertarChofer(xela, UUID.randomUUID(), "LIC-2", VENCE);

        esError(put("/fleet/choferes/" + chofer + "/licencia", tokenAdminSucursal(xela), """
                {"numeroLicencia":"LIC-2","tipoLicencia":"A","fechaVencimientoLicencia":"2031-01-31",
                 "fechaRegistro":"2026-10-20","version":0}"""), 409, "LICENCIA_EN_USO");
        assertThat(jdbc.queryForObject("SELECT numero_licencia FROM choferes WHERE id = ?", String.class, chofer))
                .isEqualTo("LIC-1");
    }

    // ----- F19: baja -----

    @Test
    void unChoferYaDadoDeBajaNoSeDaDeBajaDeNuevo() {
        UUID chofer = insertarChofer(xela, UUID.randomUUID(), "LIC-1", VENCE);
        TRIPS.responder("GET", RUTA_R3, 200, "{\"cantidad\":0,\"viajes\":[]}");

        assertThat(patch("/fleet/choferes/" + chofer + "/desactivar", tokenAdminSucursal(xela),
                "{\"fecha\":\"2026-10-20\",\"version\":0}").getStatusCode().value()).isEqualTo(200);

        // La segunda vez se corta antes de preguntarle a Trips
        esError(patch("/fleet/choferes/" + chofer + "/desactivar", tokenAdminSucursal(xela),
                "{\"fecha\":\"2026-10-20\",\"version\":1}"), 422, "CHOFER_INACTIVO");
        assertThat(TRIPS.peticiones()).hasSize(1);
    }

    // ----- PerfilActualizado (de Identity) -----

    @Test
    void unPerfilDeOtroUsuarioOConElMismoNombreNoMueveNadaNiPublica() {
        UUID chofer = insertarChofer(xela, UUID.randomUUID(), "LIC-1", VENCE);   // queda "Juan Perez"

        // Un choferId que no existe en Fleet y el mismo nombre que ya tiene: ninguno publica nada
        publicarEvento("identity.eventos", "PerfilActualizado", UUID.randomUUID(), """
                {"usuarioId":"%s","nombres":"Otro","apellidos":"Usuario","correo":"otro@fleet.gt","choferId":"%s"}
                """.formatted(UUID.randomUUID(), UUID.randomUUID()));
        publicarEvento("identity.eventos", "PerfilActualizado", UUID.randomUUID(), """
                {"usuarioId":"%s","nombres":"Juan","apellidos":"Perez","correo":"juan@fleet.gt","choferId":"%s"}
                """.formatted(UUID.randomUUID(), chofer));

        await().atMost(ESPERA).until(() -> contar("eventos_procesados") == 2);
        assertThat(contar("eventos_salida")).isZero();
        assertThat(jdbc.queryForObject("SELECT nombre_completo FROM choferes WHERE id = ?", String.class, chofer))
                .isEqualTo("Juan Perez");
    }
}
