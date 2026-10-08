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
}
