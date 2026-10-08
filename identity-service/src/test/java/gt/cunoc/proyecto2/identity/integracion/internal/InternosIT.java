package gt.cunoc.proyecto2.identity.integracion.internal;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.identity.integracion.soporte.PruebaIntegracion;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** R4: administradores de sucursal y contacto de usuarios para Notification. */
class InternosIT extends PruebaIntegracion {

    @Test
    void administradoresDeSucursal() {
        UUID sucursal = UUID.randomUUID();
        UUID solicitud = UUID.randomUUID();
        post("/identity/interno/cuentas-personal", tokenServicio(), """
                {"solicitudId":"%s","rol":"ADMIN_SUCURSAL","sucursalId":"%s","choferId":null,
                 "correo":"admin.suc@pruebas.gt","nombres":"Suc","apellidos":"Ursal",
                 "nit":"6000000-6","dpi":"6000000000606","telefono":"55550006","direccion":"Xela",
                 "fotoUrl":null}
                """.formatted(solicitud, sucursal));

        ResponseEntity<String> respuesta =
                get("/identity/interno/sucursales/" + sucursal + "/administradores", tokenServicio());
        assertThat(respuesta.getStatusCode().value() == 200).isTrue();
        assertThat(PruebaIntegracion.<java.util.List<String>>json(respuesta, "$[*].correo"))
                .contains("admin.suc@pruebas.gt");
    }

    @Test
    void contactoDeUsuario() {
        UUID usuarioId = registrarCliente("ana@pruebas.gt", "1000000-1", "1000000000101");

        ResponseEntity<String> respuesta =
                get("/identity/interno/usuarios/" + usuarioId + "/contacto", tokenServicio());
        assertThat(respuesta.getStatusCode().value() == 200).isTrue();
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.nombre")).contains("Ana");
    }

    @Test
    void contactoInexistenteDa404() {
        esError(get("/identity/interno/usuarios/" + UUID.randomUUID() + "/contacto", tokenServicio()),
                404, "USUARIO_NO_EXISTE");
    }
}
