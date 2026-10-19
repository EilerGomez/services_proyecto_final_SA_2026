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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
class UserControllerTest extends BaseControladorTest {

    @MockitoBean
    private UserService servicio;

    @Test
    void listarExigeAdminSistema() throws Exception {
        // Arrange
        sesion("token", usuario(Set.of(Roles.CLIENTE), null));
        when(servicio.listar(any(), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(fila())));

        // Act + Assert
        mvc.perform(get("/identity/usuarios").header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").exists());
    }

    @Test
    void verDeOtraSucursalDa403() throws Exception {
        // Arrange
        UUID otra = UUID.randomUUID();
        sesion("token", usuario(Set.of(Roles.ADMIN_SUCURSAL), UUID.randomUUID()));
        when(servicio.detalle(any())).thenReturn(detalle(otra));

        // Act + Assert
        mvc.perform(get("/identity/usuarios/" + UUID.randomUUID())
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("SUCURSAL_AJENA"));
    }

    @Test
    void crearAdministradorDevuelve201() throws Exception {
        // Arrange
        var actual = usuario(Set.of(Roles.ADMIN_SISTEMA), null);
        sesion("token", actual);
        when(servicio.crearAdministradorSistema(any())).thenReturn(detalle(null));

        // Act + Assert
        mvc.perform(post("/identity/usuarios/administradores-sistema")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"nuevo@correo.com","nombres":"Ana","apellidos":"López",
                                "nit":"1234567-8","dpi":"1234567890101","telefono":"55551234",
                                "direccion":"Xela"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("ana@correo.com"));
    }

    @Test
    void elAdministradorNoPuedeCambiarElEstadoDeSuPropiaCuenta() throws Exception {
        var actual = usuario(Set.of(Roles.ADMIN_SISTEMA), null);
        sesion("token", actual);

        mvc.perform(patch("/identity/usuarios/" + actual.usuarioId() + "/estado")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"INACTIVO\"}"))
                .andExpect(status().is(422))
                .andExpect(jsonPath("$.codigo").value("CUENTA_PROPIA"));
        verify(servicio, never()).cambiarEstado(any(), any());
    }

    @Test
    void cambiarEstadoDevuelve200() throws Exception {
        // Arrange
        sesion("token", usuario(Set.of(Roles.ADMIN_SISTEMA), null));
        when(servicio.cambiarEstado(any(), any())).thenReturn(detalle(null));

        // Act + Assert
        mvc.perform(patch("/identity/usuarios/" + UUID.randomUUID() + "/estado")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"estado":"INACTIVO"}"""))
                .andExpect(status().isOk());
        verify(servicio).cambiarEstado(any(), any());
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
