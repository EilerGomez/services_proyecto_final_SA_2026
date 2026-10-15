package gt.cunoc.proyecto2.fleet.integracion.personal;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.fleet.integracion.soporte.PruebaIntegracion;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** F6–F9: personal de sucursal con R5 simulado, índices únicos y el último admin. */
class PersonalSucursalIT extends PruebaIntegracion {

    private static final String RUTA_R5 = "/identity/interno/cuentas-personal";

    private UUID xela;
    private UUID adminXela;

    @BeforeEach
    void datos() {
        xela = insertarSucursal("XELA", true);
        adminXela = insertarPersonal(xela, "ADMIN_SUCURSAL");
    }

    /** R4c simulado: el estado de la cuenta del retirado en Identity. */
    private void cuentaEnIdentity(UUID personalId, String estado, String bloqueadoHasta) {
        UUID usuarioId = jdbc.queryForObject("SELECT usuario_id FROM personal_sucursal WHERE id = ?",
                UUID.class, personalId);
        IDENTITY.responder("GET", "/identity/interno/usuarios/" + usuarioId + "/estado", 200,
                "{\"usuarioId\":\"%s\",\"estado\":\"%s\",\"bloqueadoHasta\":%s}"
                        .formatted(usuarioId, estado, bloqueadoHasta == null ? "null" : "\"" + bloqueadoHasta + "\""));
    }

    private static String nuevoPersonal(String rol, UUID solicitudId) {
        return """
                {"rol":"%s","fechaAsignacion":"2026-10-20",
                 "cuenta":{"solicitudId":"%s","correo":"cajero@fleet.gt","nombres":"Luis","apellidos":"Gomez",
                           "nit":"CF","dpi":"1234567890101","telefono":"55551234","direccion":"Zona 3"}}
                """.formatted(rol, solicitudId);
    }

    @Test
    void elAdminDeSucursalAgregaUnCajero() {
        IDENTITY.responder("POST", RUTA_R5, 201, cuentaCreada(UUID.randomUUID(), "cajero@fleet.gt", "Luis Gomez"));

        ResponseEntity<String> respuesta = post("/fleet/sucursales/" + xela + "/personal", tokenAdminSucursal(xela),
                nuevoPersonal("CAJERO", UUID.randomUUID()));

        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(201);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.rol")).isEqualTo("CAJERO");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.nombreCompleto")).isEqualTo("Luis Gomez");
        assertThat(IDENTITY.peticiones().get(0).cuerpo()).contains("\"sucursalId\":\"" + xela + "\"");
        esperarEventos("PersonalSucursalAsignado");
    }

    @Test
    void unReintentoConElMismoSolicitudIdNoCreaOtraCuenta() {
        UUID solicitudId = UUID.randomUUID();
        IDENTITY.responder("POST", RUTA_R5, 201, cuentaCreada(UUID.randomUUID(), "cajero@fleet.gt", "Luis Gomez"));
        String cuerpo = nuevoPersonal("CAJERO", solicitudId);

        ResponseEntity<String> primera = post("/fleet/sucursales/" + xela + "/personal", tokenAdminSistema(), cuerpo);
        ResponseEntity<String> reintento = post("/fleet/sucursales/" + xela + "/personal", tokenAdminSistema(), cuerpo);

        assertThat(primera.getStatusCode().value()).isEqualTo(201);
        assertThat(reintento.getStatusCode().value()).isEqualTo(201);
        assertThat(PruebaIntegracion.<String>json(reintento, "$.personalId")).isEqualTo(solicitudId.toString());
        assertThat(IDENTITY.peticiones()).hasSize(1);
        assertThat(contar("personal_sucursal")).isEqualTo(2);    // admin + cajero
    }

    @Test
    void unAdminDeSucursalNoAgregaAdmins() {
        esError(post("/fleet/sucursales/" + xela + "/personal", tokenAdminSucursal(xela),
                nuevoPersonal("ADMIN_SUCURSAL", UUID.randomUUID())), 403, "SOLO_CAJEROS");
        assertThat(IDENTITY.peticiones()).isEmpty();
    }

    @Test
    void unAdminDeOtraSucursalNoVeNiAgrega() {
        UUID guate = insertarSucursal("GUATE", true);

        esError(get("/fleet/sucursales/" + xela + "/personal", tokenAdminSucursal(guate)), 403, "SUCURSAL_AJENA");
        esError(post("/fleet/sucursales/" + xela + "/personal", tokenAdminSucursal(guate),
                nuevoPersonal("CAJERO", UUID.randomUUID())), 403, "SUCURSAL_AJENA");
    }

    @Test
    void listarConFiltros() {
        insertarPersonal(xela, "CAJERO");
        insertarPersonal(xela, "CAJERO");

        ResponseEntity<String> cajeros = get("/fleet/sucursales/" + xela + "/personal?rol=CAJERO&activo=true",
                tokenAdminSucursal(xela));
        ResponseEntity<String> todos = get("/fleet/sucursales/" + xela + "/personal", tokenAdminSucursal(xela));

        assertThat(PruebaIntegracion.<List<String>>json(cajeros, "$[*].rol")).containsExactly("CAJERO", "CAJERO");
        assertThat(PruebaIntegracion.<List<String>>json(todos, "$[*].personalId")).hasSize(3);
    }

    @Test
    void desactivarUnCajero() {
        UUID cajero = insertarPersonal(xela, "CAJERO");

        ResponseEntity<String> respuesta = patch("/fleet/personal/" + cajero + "/desactivar",
                tokenAdminSucursal(xela), "{\"fechaDesasignacion\":\"2026-10-20\",\"version\":0}");

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(PruebaIntegracion.<Boolean>json(respuesta, "$.activo")).isFalse();
        esperarEventos("PersonalSucursalDesactivado");
    }

    @Test
    void noSeDesactivaAlUltimoAdmin() {
        esError(patch("/fleet/personal/" + adminXela + "/desactivar", tokenAdminSistema(),
                "{\"fechaDesasignacion\":\"2026-10-20\",\"version\":0}"), 422, "SUCURSAL_SIN_ADMIN");
    }

    @Test
    void trasladoCierraLaAsignacionYCreaLaNueva() {
        UUID guate = insertarSucursal("GUATE", true);
        UUID cajero = insertarPersonal(xela, "CAJERO");

        ResponseEntity<String> respuesta = post("/fleet/personal/" + cajero + "/traslado", tokenAdminSistema(),
                "{\"sucursalDestinoId\":\"%s\",\"fechaTraslado\":\"2026-10-20\",\"version\":0}".formatted(guate));

        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(201);
        assertThat(PruebaIntegracion.<Boolean>json(respuesta, "$.anterior.activo")).isFalse();
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.nueva.sucursalId")).isEqualTo(guate.toString());
        Integer activosEnGuate = jdbc.queryForObject(
                "SELECT count(*) FROM personal_sucursal WHERE sucursal_id = ? AND activo", Integer.class, guate);
        assertThat(activosEnGuate).isEqualTo(1);
        esperarEventos("PersonalSucursalDesactivado", "PersonalSucursalAsignado");
    }

    @Test
    void elTrasladoEsSoloDelAdminDeSistema() {
        UUID cajero = insertarPersonal(xela, "CAJERO");

        ResponseEntity<String> respuesta = post("/fleet/personal/" + cajero + "/traslado", tokenAdminSucursal(xela),
                "{\"sucursalDestinoId\":\"%s\",\"fechaTraslado\":\"2026-10-20\",\"version\":0}"
                        .formatted(UUID.randomUUID()));

        assertThat(respuesta.getStatusCode().value()).isEqualTo(403);
    }

    @Test
    void reintegrarAUnCajeroRetiradoEnOtraSucursalYConOtroPuesto() {
        UUID guate = insertarSucursal("GUATE", true);
        UUID cajero = insertarPersonal(xela, "CAJERO");
        patch("/fleet/personal/" + cajero + "/desactivar", tokenAdminSistema(),
                "{\"fechaDesasignacion\":\"2026-10-20\",\"version\":0}");
        cuentaEnIdentity(cajero, "ACTIVO", null);

        ResponseEntity<String> respuesta = post("/fleet/personal/" + cajero + "/reintegro", tokenAdminSistema(),
                "{\"sucursalId\":\"%s\",\"rol\":\"ADMIN_SUCURSAL\",\"fechaAsignacion\":\"2026-10-25\"}"
                        .formatted(guate));

        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(201);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.sucursalId")).isEqualTo(guate.toString());
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.rol")).isEqualTo("ADMIN_SUCURSAL");
        assertThat(PruebaIntegracion.<Boolean>json(respuesta, "$.activo")).isTrue();
        esperarEventos("PersonalSucursalDesactivado", "PersonalSucursalAsignado");
    }

    @Test
    void noSeReintegraAUnAdminConLaCuentaBloqueadaODesactivada() {
        UUID guate = insertarSucursal("GUATE", true);
        UUID admin = insertarPersonal(xela, "ADMIN_SUCURSAL");
        patch("/fleet/personal/" + admin + "/desactivar", tokenAdminSistema(),
                "{\"fechaDesasignacion\":\"2026-10-20\",\"version\":0}");
        String reintegro = "{\"sucursalId\":\"%s\",\"rol\":\"ADMIN_SUCURSAL\",\"fechaAsignacion\":\"2026-10-25\"}"
                .formatted(guate);

        cuentaEnIdentity(admin, "BLOQUEADO", "2099-01-01T00:00:00Z");
        esError(post("/fleet/personal/" + admin + "/reintegro", tokenAdminSistema(), reintegro),
                422, "PERSONAL_CUENTA_BLOQUEADA");

        cuentaEnIdentity(admin, "INACTIVO", null);
        esError(post("/fleet/personal/" + admin + "/reintegro", tokenAdminSistema(), reintegro),
                422, "PERSONAL_CUENTA_INACTIVA");

        Integer activas = jdbc.queryForObject(
                "SELECT count(*) FROM personal_sucursal WHERE sucursal_id = ? AND activo", Integer.class, guate);
        assertThat(activas).isZero();
    }

    @Test
    void noSeReintegraAQuienSigueActivo() {
        UUID cajero = insertarPersonal(xela, "CAJERO");

        esError(post("/fleet/personal/" + cajero + "/reintegro", tokenAdminSucursal(xela),
                "{\"sucursalId\":\"%s\",\"rol\":\"CAJERO\",\"fechaAsignacion\":\"2026-10-25\"}"
                        .formatted(xela)), 422, "PERSONAL_ACTIVO");
    }

    @Test
    void noSeReintegraSiYaTrabajaEnOtraSucursal() {
        UUID guate = insertarSucursal("GUATE", true);
        UUID cajero = insertarPersonal(xela, "CAJERO");
        // Se traslada (queda activo en GUATE) y se intenta reintegrar la fila vieja de XELA
        post("/fleet/personal/" + cajero + "/traslado", tokenAdminSistema(),
                "{\"sucursalDestinoId\":\"%s\",\"fechaTraslado\":\"2026-10-20\",\"version\":0}".formatted(guate));

        esError(post("/fleet/personal/" + cajero + "/reintegro", tokenAdminSistema(),
                "{\"sucursalId\":\"%s\",\"rol\":\"CAJERO\",\"fechaAsignacion\":\"2026-10-25\"}"
                        .formatted(xela)), 409, "PERSONAL_EN_OTRA_SUCURSAL");
    }
}
