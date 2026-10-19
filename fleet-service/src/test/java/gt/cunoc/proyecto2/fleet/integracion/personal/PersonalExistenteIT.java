package gt.cunoc.proyecto2.fleet.integracion.personal;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import gt.cunoc.proyecto2.fleet.integracion.soporte.PruebaIntegracion;
import gt.cunoc.proyecto2.fleet.integracion.soporte.ServidorFalso;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** F3 y F7 con una persona que ya tiene cuenta: R5b simulado, sin crear cuentas nuevas. */
class PersonalExistenteIT extends PruebaIntegracion {

    private static final String RUTA_R5 = "/identity/interno/cuentas-personal";
    private static final String RUTA_R5B = "/identity/interno/cuentas-personal/existente";

    private static String vinculada(UUID usuarioId) {
        return """
                {"usuarioId":"%s","correo":"ana@fleet.gt","nombreCompleto":"Ana Lopez","creada":false}
                """.formatted(usuarioId);
    }

    @Test
    void crearSucursalConUnAdminQueYaTieneCuenta() {
        UUID usuarioId = UUID.randomUUID();
        UUID solicitudId = UUID.randomUUID();
        IDENTITY.responder("POST", RUTA_R5B, 200, vinculada(usuarioId));

        ResponseEntity<String> respuesta = post("/fleet/sucursales", tokenAdminSistema(), """
                {"codigo":"XELA","nombre":"Xela Central","departamento":"Quetzaltenango","municipio":"Quetzaltenango",
                 "direccion":"4a calle 10-20 zona 1","telefono":"77651234","latitud":14.8345,"longitud":-91.518,
                 "fechaAsignacion":"2026-10-20",
                 "adminExistente":{"solicitudId":"%s","usuarioId":"%s"}}
                """.formatted(solicitudId, usuarioId));

        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(201);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.adminInicial.personalId"))
                .isEqualTo(solicitudId.toString());
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.adminInicial.nombreCompleto")).isEqualTo("Ana Lopez");

        assertThat(IDENTITY.peticiones()).hasSize(1);
        ServidorFalso.Peticion r5b = IDENTITY.peticiones().get(0);
        assertThat(r5b.ruta()).isEqualTo(RUTA_R5B);
        assertThat(r5b.cuerpo()).contains("\"rol\":\"ADMIN_SUCURSAL\"").contains("\"sucursalNombre\":\"Xela Central\"");

        Map<String, String> eventos = esperarEventos("SucursalCreada", "PersonalSucursalAsignado");
        assertThat(JsonPath.<String>read(eventos.get("PersonalSucursalAsignado"), "$.payload.sucursalNombre"))
                .isEqualTo("Xela Central");
    }

    @Test
    void sinAdminInicialEsSolicitudInvalidaYNoSeGuardaNada() {
        ResponseEntity<String> respuesta = post("/fleet/sucursales", tokenAdminSistema(), """
                {"codigo":"XELA","nombre":"Xela Central","departamento":"Quetzaltenango","municipio":"Quetzaltenango",
                 "direccion":"4a calle","telefono":null,"latitud":14.8345,"longitud":-91.518,
                 "fechaAsignacion":"2026-10-20"}
                """);

        esError(respuesta, 400, "CUENTA_REQUERIDA");
        assertThat(contar("sucursales")).isZero();
        assertThat(IDENTITY.peticiones()).isEmpty();
    }

    @Test
    void agregarUnCajeroQueYaTieneCuenta() {
        UUID xela = insertarSucursal("XELA", true);
        insertarPersonal(xela, "ADMIN_SUCURSAL");
        UUID usuarioId = UUID.randomUUID();
        IDENTITY.responder("POST", RUTA_R5B, 200, vinculada(usuarioId));

        ResponseEntity<String> respuesta = post("/fleet/sucursales/" + xela + "/personal", tokenAdminSucursal(xela),
                """
                {"rol":"CAJERO","fechaAsignacion":"2026-10-20",
                 "usuarioExistente":{"solicitudId":"%s","usuarioId":"%s"}}
                """.formatted(UUID.randomUUID(), usuarioId));

        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(201);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.usuarioId")).isEqualTo(usuarioId.toString());
        assertThat(IDENTITY.peticiones()).extracting(ServidorFalso.Peticion::ruta).containsExactly(RUTA_R5B);
        assertThat(IDENTITY.peticiones()).extracting(ServidorFalso.Peticion::ruta).doesNotContain(RUTA_R5);
    }

    @Test
    void siIdentityRechazaLaCuentaNoSeGuardaNada() {
        UUID xela = insertarSucursal("XELA", true);
        IDENTITY.responder("POST", RUTA_R5B, 422,
                errorRemoto(422, "ES_CHOFER", "La cuenta pertenece a un chofer; no puede ser personal de sucursal"));

        ResponseEntity<String> respuesta = post("/fleet/sucursales/" + xela + "/personal", tokenAdminSistema(),
                """
                {"rol":"CAJERO","fechaAsignacion":"2026-10-20",
                 "usuarioExistente":{"usuarioId":"%s"}}
                """.formatted(UUID.randomUUID()));

        esError(respuesta, 422, "ES_CHOFER");
        assertThat(contar("personal_sucursal")).isZero();
    }
}
