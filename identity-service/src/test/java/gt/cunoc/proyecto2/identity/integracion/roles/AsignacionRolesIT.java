package gt.cunoc.proyecto2.identity.integracion.roles;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.integracion.soporte.PruebaIntegracion;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Roles para cuentas que ya existen: búsqueda por correo, R5b (personal de sucursal desde Fleet)
 * y promoción a administrador del sistema. Las dos asignaciones publican RolAsignado.
 */
class AsignacionRolesIT extends PruebaIntegracion {

    private static final UUID SUCURSAL = UUID.randomUUID();
    private static final String R5B = "/identity/interno/cuentas-personal/existente";

    @Test
    void clienteExistenteSeAsignaComoAdminDeSucursalYSeLeAvisa() {
        UUID clienteId = registrarCliente("ana@pruebas.gt", "1000000-1", "1000000000101");

        ResponseEntity<String> busqueda = get("/identity/usuarios/por-correo?correo=ANA@pruebas.gt", tokenAdmin);
        assertThat(busqueda.getStatusCode().value()).as(busqueda.getBody()).isEqualTo(200);
        assertThat(PruebaIntegracion.<String>json(busqueda, "$.usuarioId")).isEqualTo(clienteId.toString());
        assertThat(PruebaIntegracion.<Boolean>json(busqueda, "$.puedeSerPersonal")).isTrue();

        UUID solicitud = UUID.randomUUID();
        String cuerpo = """
                {"solicitudId":"%s","usuarioId":"%s","rol":"ADMIN_SUCURSAL","sucursalId":"%s",
                 "sucursalNombre":"Xela Central"}
                """.formatted(solicitud, clienteId, SUCURSAL);
        ResponseEntity<String> vinculada = post(R5B, tokenServicio(), cuerpo);
        assertThat(vinculada.getStatusCode().value()).as(vinculada.getBody()).isEqualTo(200);
        assertThat(PruebaIntegracion.<String>json(vinculada, "$.nombreCompleto")).isEqualTo("Ana López");
        // R5b solo valida: el rol llega con el evento de Fleet
        assertThat(PruebaIntegracion.<List<String>>json(get("/identity/usuarios/" + clienteId, tokenAdmin), "$.roles"))
                .containsExactly(Roles.CLIENTE);

        // El reintento devuelve lo mismo
        assertThat(post(R5B, tokenServicio(), cuerpo).getStatusCode().value()).isEqualTo(200);

        // Fleet guarda la fila y publica PersonalSucursalAsignado: Identity pone el rol y avisa (RolAsignado)
        publicarEvento("fleet.eventos", "PersonalSucursalAsignado", solicitud, """
                {"personalId":"%s","usuarioId":"%s","sucursalId":"%s","rolCodigo":"ADMIN_SUCURSAL",
                 "fechaAsignacion":"2026-10-20","sucursalNombre":"Xela Central"}
                """.formatted(solicitud, clienteId, SUCURSAL));
        Map<String, String> eventos = esperarEventos("RolAsignado");

        ResponseEntity<String> detalle = get("/identity/usuarios/" + clienteId, tokenAdmin);
        assertThat(PruebaIntegracion.<List<String>>json(detalle, "$.roles"))
                .containsExactlyInAnyOrder(Roles.ADMIN_SUCURSAL, Roles.CLIENTE);
        assertThat(PruebaIntegracion.<String>json(detalle, "$.sucursalId")).isEqualTo(SUCURSAL.toString());

        String sobre = eventos.get("RolAsignado");
        assertThat(JsonPath.<String>read(sobre, "$.payload.rol")).isEqualTo(Roles.ADMIN_SUCURSAL);
        assertThat(JsonPath.<String>read(sobre, "$.payload.sucursalNombre")).isEqualTo("Xela Central");
        assertThat(JsonPath.<String>read(sobre, "$.payload.correo")).isEqualTo("ana@pruebas.gt");

        // Ya es personal: la búsqueda lo indica y una segunda asignación se rechaza
        ResponseEntity<String> otraVez = get("/identity/usuarios/por-correo?correo=ana@pruebas.gt", tokenAdmin);
        assertThat(PruebaIntegracion.<Boolean>json(otraVez, "$.puedeSerPersonal")).isFalse();
        esError(post(R5B, tokenServicio(), cuerpo.replace(solicitud.toString(), UUID.randomUUID().toString())),
                409, "YA_ES_PERSONAL");
    }

    @Test
    void busquedaDeCorreoInexistenteDa404() {
        esError(get("/identity/usuarios/por-correo?correo=nadie@pruebas.gt", tokenAdmin), 404, "USUARIO_NO_EXISTE");
    }

    @Test
    void clienteSePromueveAAdministradorDelSistemaUnaSolaVez() {
        UUID clienteId = registrarCliente("luis@pruebas.gt", "2000000-2", "2000000000202");

        ResponseEntity<String> promovido = post("/identity/usuarios/" + clienteId + "/administrador-sistema",
                tokenAdmin, null);
        assertThat(promovido.getStatusCode().value()).as(promovido.getBody()).isEqualTo(200);
        assertThat(PruebaIntegracion.<List<String>>json(promovido, "$.roles"))
                .containsExactlyInAnyOrder(Roles.ADMIN_SISTEMA, Roles.CLIENTE);

        Map<String, String> eventos = esperarEventos("RolAsignado");
        assertThat(JsonPath.<String>read(eventos.get("RolAsignado"), "$.payload.rol")).isEqualTo(Roles.ADMIN_SISTEMA);

        esError(post("/identity/usuarios/" + clienteId + "/administrador-sistema", tokenAdmin, null),
                409, "YA_ES_ADMIN_SISTEMA");
    }

    @Test
    void clienteNoPuedePromoverNiBuscar() {
        UUID clienteId = registrarCliente("eva@pruebas.gt", "3000000-3", "3000000000303");
        String token = tokenCliente(clienteId, "eva@pruebas.gt");

        esError(post("/identity/usuarios/" + clienteId + "/administrador-sistema", token, null),
                403, "ROL_INSUFICIENTE");
        esError(get("/identity/usuarios/por-correo?correo=eva@pruebas.gt", token), 403, "ROL_INSUFICIENTE");
    }
}
