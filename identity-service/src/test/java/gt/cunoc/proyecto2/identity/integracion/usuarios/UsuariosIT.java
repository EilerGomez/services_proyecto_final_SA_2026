package gt.cunoc.proyecto2.identity.integracion.usuarios;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.identity.integracion.soporte.PruebaIntegracion;
import java.util.Map;
import java.util.Set;
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
    void nadieCambiaElEstadoDeSuPropiaCuenta() {
        ResponseEntity<String> respuesta = patch("/identity/usuarios/" + adminId + "/estado",
                tokenAdmin, "{\"estado\":\"INACTIVO\"}");
        esError(respuesta, 422, "CUENTA_PROPIA");
    }

    @Test
    void desactivarAlUltimoAdminSistemaSeRechaza() {
        // Otro administrador (solo en el token) intenta desactivar al único admin activo de la base
        String tokenOtroAdmin = jwt.generar(new UsuarioActual(UUID.randomUUID(), "otro@pruebas.gt",
                Set.of(Roles.ADMIN_SISTEMA, Roles.CLIENTE), null, null));
        ResponseEntity<String> respuesta = patch("/identity/usuarios/" + adminId + "/estado",
                tokenOtroAdmin, "{\"estado\":\"INACTIVO\"}");
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
    void desactivarUnaCuentaRevocaSusSesionesAlInstante() {
        UUID usuarioId = registrarCliente("revocar@pruebas.gt", "5000000-5", "5000000000505");

        patch("/identity/usuarios/" + usuarioId + "/estado", tokenAdmin, "{\"estado\":\"INACTIVO\"}");

        // El API Gateway recibe el evento y, si estaba apagado, lo carga del endpoint interno al arrancar
        assertThat(esperarEventos("SesionRevocada").get("SesionRevocada")).contains(usuarioId.toString());
        ResponseEntity<String> revocadas = get("/identity/interno/sesiones-revocadas", tokenServicio());
        assertThat(revocadas.getStatusCode().value()).isEqualTo(200);
        assertThat(PruebaIntegracion.<java.util.List<String>>json(revocadas, "$[*].usuarioId"))
                .contains(usuarioId.toString());
    }

    @Test
    void elBloqueoDelAdministradorNoVenceHastaQueLoActiva() {
        UUID usuarioId = registrarCliente("bloqueo@pruebas.gt", "4000000-4", "4000000000404");
        String login = "{\"correo\":\"bloqueo@pruebas.gt\",\"contrasena\":\"%s\"}".formatted(CONTRASENA);

        patch("/identity/usuarios/" + usuarioId + "/estado", tokenAdmin, "{\"estado\":\"BLOQUEADO\"}");
        // Con la contraseña correcta sigue bloqueada (antes se reactivaba sola)
        esError(post("/identity/auth/login", null, login), 401, "CUENTA_BLOQUEADA");
        esError(post("/identity/auth/login", null, login), 401, "CUENTA_BLOQUEADA");

        patch("/identity/usuarios/" + usuarioId + "/estado", tokenAdmin, "{\"estado\":\"ACTIVO\"}");
        assertThat(post("/identity/auth/login", null, login).getStatusCode().value()).isEqualTo(200);
    }

    @Test
    void clienteNoPuedeListarUsuarios() {
        UUID usuarioId = registrarCliente("ana@pruebas.gt", "1000000-1", "1000000000101");
        String token = tokenCliente(usuarioId, "ana@pruebas.gt");

        esError(get("/identity/usuarios", token), 403, "ROL_INSUFICIENTE");
    }
}
