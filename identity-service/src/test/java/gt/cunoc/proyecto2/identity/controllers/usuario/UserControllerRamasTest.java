package gt.cunoc.proyecto2.identity.controllers.usuario;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.controllers.BaseControladorTest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioDetalleResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioListaResponse;
import gt.cunoc.proyecto2.identity.services.usuario.UserService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ramas de UserController sin cubrir: listado exitoso y detalle permitido. */
@WebMvcTest(UserController.class)
class UserControllerRamasTest extends BaseControladorTest {

    @MockitoBean
    private UserService servicio;

    @Test
    void listarComoAdminDevuelve200() throws Exception {
        // Arrange
        sesion("token", usuario(Set.of(Roles.ADMIN_SISTEMA), null));
        when(servicio.listar(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(fila())));
        // Act + Assert
        mvc.perform(get("/identity/usuarios").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correo").value("ana@correo.com"));
    }

    @Test
    void verComoAdminSistemaDevuelve200() throws Exception {
        // Arrange
        sesion("token", usuario(Set.of(Roles.ADMIN_SISTEMA), null));
        when(servicio.detalle(any())).thenReturn(detalle(UUID.randomUUID()));

        // Act + Assert
        mvc.perform(get("/identity/usuarios/" + UUID.randomUUID())
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ana@correo.com"));
    }

    @Test
    void verSinSucursalDa403() throws Exception {
        // Arrange
        sesion("token", usuario(Set.of(Roles.ADMIN_SUCURSAL), UUID.randomUUID()));
        when(servicio.detalle(any())).thenReturn(detalle(null));

        // Act + Assert
        mvc.perform(get("/identity/usuarios/" + UUID.randomUUID())
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SUCURSAL_AJENA"));
    }

    @Test
    void verMismaSucursalDevuelve200() throws Exception {
        // Arrange
        UUID sucursalId = UUID.randomUUID();
        sesion("token", usuario(Set.of(Roles.ADMIN_SUCURSAL), sucursalId));
        when(servicio.detalle(any())).thenReturn(detalle(sucursalId));

        // Act + Assert
        mvc.perform(get("/identity/usuarios/" + UUID.randomUUID())
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ana@correo.com"));
    }

    private UsuarioListaResponse fila() {
        return new UsuarioListaResponse(UUID.randomUUID(), "ana@correo.com", "Ana López",
                "1234567-8", Set.of(Roles.CLIENTE), "ACTIVO", null, Instant.now());
    }

    private UsuarioDetalleResponse detalle(UUID sucursalId) {
        return new UsuarioDetalleResponse(UUID.randomUUID(), "ana@correo.com", "Ana", "López",
                "1234567-8", "1234567890101", "55551234", "Xela", null,
                Set.of(Roles.CLIENTE), sucursalId, null, "ACTIVO", Instant.now());
    }
}
