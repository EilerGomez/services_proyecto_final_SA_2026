package gt.cunoc.proyecto2.identity.integracion.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.identity.integracion.soporte.PruebaIntegracion;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** Administración de usuarios: altas, estados y regla del último admin. */
class UsuariosIT extends PruebaIntegracion {

    @Test
    void crearAdministradorSistemaPublicaCuentaConActivacion() {
        ResponseEntity<String> respuesta = post("/identity/usuarios/administradores-sistema", tokenAdmin, """
                {"correo":"otro.admin@pruebas.gt","nombres":"Otro","apellidos":"Admin",
                 "nit":"2000000-2","dpi":"2000000000202","telefono":"55550002","direccion":"Guate"}
                """);
        assertThat(respuesta.getStatusCode().value() == 201).isTrue();

        Map<String, String> eventos = esperarEventos("CuentaCreada", "ClienteRegistrado");
        assertThat(eventos.get("CuentaCreada")).contains("/restablecer?token=");
    }

    @Test
    void desactivarAlUltimoAdminSistemaSeRechaza() {
        ResponseEntity<String> respuesta = patch("/identity/usuarios/" + adminId + "/estado",
                tokenAdmin, "{\"estado\":\"INACTIVO\"}");
        esError(respuesta, 422, "ULTIMO_ADMIN_SISTEMA");
    }

    @Test
    void listarConFiltrosYVerDetalle() {
        registrarCliente("ana@pruebas.gt", "1000000-1", "1000000000101");

        ResponseEntity<String> lista = get("/identity/usuarios?rol=CLIENTE&estado=ACTIVO&q=ana", tokenAdmin);
        assertThat(lista.getStatusCode().value() == 200).isTrue();
        assertThat(PruebaIntegracion.<java.util.List<String>>json(lista, "$[*].correo"))
                .contains("ana@pruebas.gt");

        UUID otro = registrarCliente("luis@pruebas.gt", "3000000-3", "3000000000303");
        ResponseEntity<String> detalle = get("/identity/usuarios/" + otro, tokenAdmin);
        assertThat(detalle.getStatusCode().value() == 200).isTrue();
        assertThat(PruebaIntegracion.<String>json(detalle, "$.estado")).isEqualTo("ACTIVO");
    }

    @Test
    void clienteNoPuedeListarUsuarios() {
        UUID usuarioId = registrarCliente("ana@pruebas.gt", "1000000-1", "1000000000101");
        String token = tokenCliente(usuarioId, "ana@pruebas.gt");

        esError(get("/identity/usuarios", token), 403, "ROL_INSUFICIENTE");
    }
}
