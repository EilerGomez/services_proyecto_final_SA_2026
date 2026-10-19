package gt.cunoc.proyecto2.fleet.integracion.sucursales;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import gt.cunoc.proyecto2.fleet.integracion.soporte.PruebaIntegracion;
import gt.cunoc.proyecto2.fleet.integracion.soporte.ServidorFalso;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** F1–F5 de punta a punta: HTTP → seguridad → servicio → PostgreSQL → outbox → RabbitMQ. */
class SucursalesIT extends PruebaIntegracion {

    private static final String RUTA_R5 = "/identity/interno/cuentas-personal";

    private static String nuevaSucursal(String codigo, UUID solicitudId) {
        return """
                {"codigo":"%s","nombre":"Xela Central","departamento":"Quetzaltenango","municipio":"Quetzaltenango",
                 "direccion":"4a calle 10-20 zona 1","telefono":"77651234","latitud":14.8345,"longitud":-91.518,
                 "fechaAsignacion":"2026-10-20",
                 "adminInicial":{"solicitudId":"%s","correo":"Admin.Xela@Fleet.gt","nombres":"Ana","apellidos":"Lopez",
                                 "nit":"1234567-8","dpi":"1234567890101","telefono":"55551234","direccion":"Zona 1"}}
                """.formatted(codigo, solicitudId);
    }

    private static String actualizacion(String nombre, int version) {
        return """
                {"nombre":"%s","departamento":"Quetzaltenango","municipio":"Salcaja","direccion":"Km 200",
                 "telefono":null,"latitud":14.9,"longitud":-91.5,"version":%d}
                """.formatted(nombre, version);
    }

    // ----- F3 -----

    @Test
    void crearSucursalConSuAdminInicialYPublicarLosEventos() {
        UUID usuarioId = UUID.randomUUID();
        UUID solicitudId = UUID.randomUUID();
        IDENTITY.responder("POST", RUTA_R5, 201, cuentaCreada(usuarioId, "admin.xela@fleet.gt", "Ana Lopez"));

        ResponseEntity<String> respuesta = post("/fleet/sucursales", tokenAdminSistema(),
                nuevaSucursal("xela", solicitudId));

        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(201);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.sucursal.codigo")).isEqualTo("XELA");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.adminInicial.personalId"))
                .isEqualTo(solicitudId.toString());
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.adminInicial.usuarioId"))
                .isEqualTo(usuarioId.toString());
        assertThat(contar("sucursales")).isEqualTo(1);
        assertThat(contar("personal_sucursal")).isEqualTo(1);

        // R5: token de servicio y datos normalizados
        ServidorFalso.Peticion r5 = IDENTITY.peticiones().get(0);
        assertThat(r5.autorizacion()).startsWith("Bearer ");
        assertThat(r5.cuerpo()).contains("\"rol\":\"ADMIN_SUCURSAL\"").contains("admin.xela@fleet.gt");

        // Outbox → relay → RabbitMQ, con la fecha de negocio elegida por el usuario
        Map<String, String> eventos = esperarEventos("SucursalCreada", "PersonalSucursalAsignado");
        String sucursalCreada = eventos.get("SucursalCreada");
        assertThat(JsonPath.<String>read(sucursalCreada, "$.payload.codigo")).isEqualTo("XELA");
        assertThat(JsonPath.<String>read(sucursalCreada, "$.businessDate")).isEqualTo("2026-10-20");
        assertThat(JsonPath.<String>read(eventos.get("PersonalSucursalAsignado"), "$.payload.rolCodigo"))
                .isEqualTo("ADMIN_SUCURSAL");
    }

    @Test
    void siIdentityRechazaElCorreoNoSeGuardaNada() {
        IDENTITY.responder("POST", RUTA_R5, 409, errorRemoto(409, "CORREO_EN_USO", "El correo ya existe"));

        ResponseEntity<String> respuesta = post("/fleet/sucursales", tokenAdminSistema(),
                nuevaSucursal("XELA", UUID.randomUUID()));

        esError(respuesta, 409, "CORREO_EN_USO");
        assertThat(contar("sucursales")).isZero();           // rollback de toda la transacción
        assertThat(contar("eventos_salida")).isZero();       // tampoco quedan eventos
    }

    @Test
    void siIdentityFallaRespondeServicioNoDisponible() {
        IDENTITY.responder("POST", RUTA_R5, 500, errorRemoto(500, "ERROR_INTERNO", "falla"));

        ResponseEntity<String> respuesta = post("/fleet/sucursales", tokenAdminSistema(),
                nuevaSucursal("XELA", UUID.randomUUID()));

        esError(respuesta, 503, "IDENTITY_NO_DISPONIBLE");
        assertThat(contar("sucursales")).isZero();
    }

    @Test
    void codigoRepetidoEs409() {
        insertarSucursal("XELA", true);

        esError(post("/fleet/sucursales", tokenAdminSistema(), nuevaSucursal("XELA", UUID.randomUUID())),
                409, "SUCURSAL_DUPLICADA");
        assertThat(IDENTITY.peticiones()).isEmpty();
    }

    @Test
    void cuerpoInvalidoEs400ConLosCampos() {
        ResponseEntity<String> respuesta = post("/fleet/sucursales", tokenAdminSistema(),
                "{\"codigo\":\"X\",\"latitud\":200}");

        esError(respuesta, 400, "VALIDACION");
        List<String> campos = json(respuesta, "$.errores[*].campo");
        // adminInicial ya no es obligatorio aquí: puede venir adminExistente (lo valida el servicio: CUENTA_REQUERIDA)
        assertThat(campos).contains("nombre", "latitud");
    }

    @Test
    void soloElAdminDeSistemaCreaSucursales() {
        String cuerpo = nuevaSucursal("XELA", UUID.randomUUID());

        assertThat(post("/fleet/sucursales", null, cuerpo).getStatusCode().value()).isEqualTo(401);
        assertThat(post("/fleet/sucursales", tokenCliente(), cuerpo).getStatusCode().value()).isEqualTo(403);
        assertThat(post("/fleet/sucursales", "token-falso", cuerpo).getStatusCode().value()).isEqualTo(401);
    }

    // ----- F1 y F2 (públicas) -----

    @Test
    void listarYObtenerSinSesion() {
        UUID xela = insertarSucursal("XELA", true);
        insertarSucursal("GUATE", false);

        ResponseEntity<String> activas = get("/fleet/sucursales?activa=true", null);
        ResponseEntity<String> todas = get("/fleet/sucursales", null);

        assertThat(activas.getStatusCode().value()).isEqualTo(200);
        assertThat(PruebaIntegracion.<List<String>>json(activas, "$[*].codigo")).containsExactly("XELA");
        assertThat(PruebaIntegracion.<List<String>>json(todas, "$[*].codigo")).hasSize(2);
        assertThat(PruebaIntegracion.<String>json(get("/fleet/sucursales/" + xela, null), "$.nombre"))
                .isEqualTo("Sucursal XELA");
        esError(get("/fleet/sucursales/" + UUID.randomUUID(), null), 404, "SUCURSAL_NO_EXISTE");
    }

    // ----- F4 -----

    @Test
    void actualizarSubeLaVersionYPublica() {
        UUID xela = insertarSucursal("XELA", true);

        ResponseEntity<String> respuesta = put("/fleet/sucursales/" + xela, tokenAdminSistema(),
                actualizacion("Xela Norte", 0));

        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.nombre")).isEqualTo("Xela Norte");
        assertThat(PruebaIntegracion.<Integer>json(respuesta, "$.version")).isEqualTo(1);
        esperarEventos("SucursalActualizada");
    }

    @Test
    void actualizarConVersionViejaEs409() {
        UUID xela = insertarSucursal("XELA", true);

        esError(put("/fleet/sucursales/" + xela, tokenAdminSistema(), actualizacion("Xela Norte", 3)),
                409, "MODIFICACION_CONCURRENTE");
    }

    // ----- F5 -----

    @Test
    void noSeDesactivaConBusesQueNoEstanInactivos() {
        UUID xela = insertarSucursal("XELA", true);
        insertarBus(xela, "P-111AAA", "EN_TALLER", "0");

        esError(patch("/fleet/sucursales/" + xela + "/estado", tokenAdminSistema(),
                "{\"activa\":false,\"version\":0}"), 422, "SUCURSAL_CON_BUSES_ACTIVOS");
    }

    @Test
    void desactivarSinBusesActivos() {
        UUID xela = insertarSucursal("XELA", true);
        insertarBus(xela, "P-111AAA", "INACTIVO", "0");

        ResponseEntity<String> respuesta = patch("/fleet/sucursales/" + xela + "/estado", tokenAdminSistema(),
                "{\"activa\":false,\"version\":0}");

        assertThat(respuesta.getStatusCode().value()).isEqualTo(200);
        assertThat(PruebaIntegracion.<Boolean>json(respuesta, "$.activa")).isFalse();
        esperarEventos("SucursalActualizada");
    }
}
