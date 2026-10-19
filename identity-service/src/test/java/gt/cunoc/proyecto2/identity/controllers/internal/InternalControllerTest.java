package gt.cunoc.proyecto2.identity.controllers.internal;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.controllers.BaseControladorTest;
import gt.cunoc.proyecto2.identity.dtos.usuario.AdminContactoResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.ContactoResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.EstadoCuentaResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.SesionRevocadaResponse;
import gt.cunoc.proyecto2.identity.services.usuario.UserService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalController.class)
class InternalControllerTest extends BaseControladorTest {

    @MockitoBean
    private UserService servicio;

    @Test
    void administradoresDevuelveArreglo() throws Exception {
        // Arrange
        sesion("token", usuario(java.util.Set.of(Roles.SERVICIO), null));
        when(servicio.administradoresDe(any())).thenReturn(List.of(
                new AdminContactoResponse(UUID.randomUUID(), "Luis Gómez", "luis@empresa.com")));

        // Act + Assert
        mvc.perform(get("/identity/interno/sucursales/" + UUID.randomUUID() + "/administradores")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correo").value("luis@empresa.com"));
    }

    @Test
    void sesionesRevocadasDeLaUltimaHora() throws Exception {
        // Arrange
        sesion("token", usuario(java.util.Set.of(Roles.SERVICIO), null));
        UUID usuarioId = UUID.randomUUID();
        java.time.Instant desde = java.time.Instant.parse("2026-10-20T15:00:00Z");
        org.mockito.ArgumentCaptor<java.time.Instant> limite = org.mockito.ArgumentCaptor.forClass(java.time.Instant.class);
        when(servicio.sesionesRevocadasDesde(limite.capture()))
                .thenReturn(List.of(new SesionRevocadaResponse(usuarioId, desde)));
        java.time.Instant antes = java.time.Instant.now();

        // Act + Assert
        mvc.perform(get("/identity/interno/sesiones-revocadas").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioId").value(usuarioId.toString()))
                .andExpect(jsonPath("$[0].tokensValidosDesde").exists());
        // Solo las que aún pueden afectar a un token vigente (dura 60 minutos)
        org.assertj.core.api.Assertions.assertThat(limite.getValue())
                .isBetween(antes.minusSeconds(3601), java.time.Instant.now().minusSeconds(3599));
    }

    @Test
    void contactoDevuelveDatos() throws Exception {
        // Arrange
        sesion("token", usuario(java.util.Set.of(Roles.SERVICIO), null));
        when(servicio.contactoDe(any())).thenReturn(
                new ContactoResponse(UUID.randomUUID(), "Pedro Ramírez", "pedro@correo.com", "55553333"));

        // Act + Assert
        mvc.perform(get("/identity/interno/usuarios/" + UUID.randomUUID() + "/contacto")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("55553333"));
    }

    @Test
    void estadoCuentaDevuelveEstadoYBloqueo() throws Exception {
        // Arrange
        sesion("token", usuario(java.util.Set.of(Roles.SERVICIO), null));
        when(servicio.estadoCuentaDe(any())).thenReturn(new EstadoCuentaResponse(
                UUID.randomUUID(), "BLOQUEADO", java.time.Instant.parse("2026-10-20T15:00:00Z")));

        // Act + Assert
        mvc.perform(get("/identity/interno/usuarios/" + UUID.randomUUID() + "/estado")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BLOQUEADO"))
                .andExpect(jsonPath("$.bloqueadoHasta").value("2026-10-20T15:00:00Z"));
    }
}
