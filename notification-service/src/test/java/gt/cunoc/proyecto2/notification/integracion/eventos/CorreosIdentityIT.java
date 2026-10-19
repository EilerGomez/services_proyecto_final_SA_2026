package gt.cunoc.proyecto2.notification.integracion.eventos;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import gt.cunoc.proyecto2.notification.integracion.soporte.PruebaIntegracion;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Correos de Identity: bienvenida (cliente o con puesto), rol asignado, recuperación y código de doble factor. */
class CorreosIdentityIT extends PruebaIntegracion {

    private static final Duration ESPERA = Duration.ofSeconds(15);

    @Test
    void cuentaCreadaEnviaLaBienvenidaConElEnlace() {
        String payload = """
                {"usuarioId":"%s","correo":"cajero@correo.com","nombre":"Luis","roles":["CAJERO"],
                 "urlActivacion":"http://localhost:5173/activar?token=abc"}
                """.formatted(UUID.randomUUID());

        UUID eventId = publicarEvento(IDENTITY_EVENTOS, "CuentaCreada", "2026-11-01", payload);

        await().atMost(ESPERA).until(() -> procesado(eventId));
        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(asuntosPara("cajero@correo.com")).containsExactly("Bienvenido a la plataforma"));
        assertThat(textoPara("cajero@correo.com")).contains("Hola Luis", "activar?token=abc");
        assertThat(notificaciones(eventId).get(0)).containsEntry("plantilla", "CUENTA_CREADA")
                .containsEntry("estado", "ENVIADA");
    }

    @Test
    void cuentaSinEnlaceSoloInvitaAIniciarSesion() {
        String payload = """
                {"usuarioId":"%s","correo":"ana@correo.com","nombre":"Ana","roles":["CLIENTE"],"urlActivacion":null}
                """.formatted(UUID.randomUUID());

        UUID eventId = publicarEvento(IDENTITY_EVENTOS, "CuentaCreada", "2026-11-01", payload);

        await().atMost(ESPERA).until(() -> procesado(eventId));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(asuntosPara("ana@correo.com")).hasSize(1));
        assertThat(textoPara("ana@correo.com")).contains("Ya puedes iniciar sesión");
    }

    @Test
    void recuperacionEnviaElEnlace() {
        String payload = """
                {"usuarioId":"%s","correo":"ana@correo.com","nombre":"Ana",
                 "urlRecuperacion":"http://localhost:5173/restablecer?token=xyz","expiraEn":"2026-11-01T15:00:00Z"}
                """.formatted(UUID.randomUUID());

        UUID eventId = publicarEvento(IDENTITY_EVENTOS, "RecuperacionContrasenaSolicitada", "2026-11-01", payload);

        await().atMost(ESPERA).until(() -> procesado(eventId));
        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(asuntosPara("ana@correo.com")).containsExactly("Recupera tu contraseña"));
        assertThat(textoPara("ana@correo.com")).contains("restablecer?token=xyz", "01/11/2026 09:00");
    }

    @Test
    void codigoDobleFactorEnviaElCodigo() {
        String payload = """
                {"usuarioId":"%s","correo":"admin@correo.com","nombre":"Admin","codigo":"482913",
                 "expiraEn":"2026-11-01T15:10:00Z"}
                """.formatted(UUID.randomUUID());

        UUID eventId = publicarEvento(IDENTITY_EVENTOS, "CodigoDobleFactorSolicitado", "2026-11-01", payload);

        await().atMost(ESPERA).until(() -> procesado(eventId));
        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(asuntosPara("admin@correo.com")).containsExactly("Tu código de verificación"));
        assertThat(textoPara("admin@correo.com")).contains("482913", "01/11/2026 09:10");
    }

    @Test
    void cuentaDePersonalDiceElPuestoYLaSucursal() {
        String payload = """
                {"usuarioId":"%s","correo":"admin.xela@correo.com","nombre":"Luis","roles":["ADMIN_SUCURSAL","CLIENTE"],
                 "urlActivacion":"http://localhost:5173/restablecer?token=abc","sucursalNombre":"Xela Central"}
                """.formatted(UUID.randomUUID());

        UUID eventId = publicarEvento(IDENTITY_EVENTOS, "CuentaCreada", "2026-11-01", payload);

        await().atMost(ESPERA).until(() -> procesado(eventId));
        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(asuntosPara("admin.xela@correo.com")).containsExactly("Bienvenido a la plataforma"));
        assertThat(textoPara("admin.xela@correo.com"))
                .contains("Administrador de sucursal en «Xela Central»", "restablecer?token=abc");
        assertThat(notificaciones(eventId).get(0)).containsEntry("plantilla", "CUENTA_CREADA");
    }

    @Test
    void rolAsignadoPideVolverAIniciarSesion() {
        String payload = """
                {"usuarioId":"%s","correo":"ana@correo.com","nombre":"Ana","rol":"ADMIN_SISTEMA",
                 "sucursalId":null,"sucursalNombre":null}
                """.formatted(UUID.randomUUID());

        UUID eventId = publicarEvento(IDENTITY_EVENTOS, "RolAsignado", "2026-11-01", payload);

        await().atMost(ESPERA).until(() -> procesado(eventId));
        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(asuntosPara("ana@correo.com")).containsExactly("Tienes un nuevo rol en la plataforma"));
        assertThat(textoPara("ana@correo.com"))
                .contains("Administrador del sistema", "cierra sesión y vuelve a iniciar sesión");
        assertThat(notificaciones(eventId).get(0)).containsEntry("plantilla", "ROL_ASIGNADO")
                .containsEntry("estado", "ENVIADA");
    }
}
