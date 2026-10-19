package gt.cunoc.proyecto2.identity.controllers.usuario;

import gt.cunoc.proyecto2.comun.errores.ConflictoException;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.controllers.BaseControladorTest;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioAsignableResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioBusquedaResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.UsuarioDetalleResponse;
import gt.cunoc.proyecto2.identity.models.PuestoAsignable;
import gt.cunoc.proyecto2.identity.services.usuario.UserService;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Búsqueda por correo y promoción a administrador del sistema. */
@WebMvcTest(UserController.class)
class UserControllerRolesTest extends BaseControladorTest {

    @MockitoBean
    private UserService servicio;

    @Test
    void adminSucursalPuedeBuscarPorCorreo() throws Exception {
        // Arrange
        sesion("token", usuario(Set.of(Roles.ADMIN_SUCURSAL), UUID.randomUUID()));
        when(servicio.buscarPorCorreo("ana@correo.com", false, PuestoAsignable.PERSONAL)).thenReturn(new UsuarioBusquedaResponse(
                UUID.randomUUID(), "ana@correo.com", "Ana López", null, true, null));

        // Act + Assert
        mvc.perform(get("/identity/usuarios/por-correo").param("correo", "ana@correo.com")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombreCompleto").value("Ana López"))
                .andExpect(jsonPath("$.puedeSerPersonal").value(true));
    }

    @Test
    void clienteNoPuedeBuscarPorCorreo() throws Exception {
        // Arrange
        sesion("token", usuario(Set.of(Roles.CLIENTE), null));

        // Act + Assert
        mvc.perform(get("/identity/usuarios/por-correo").param("correo", "ana@correo.com")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
        verify(servicio, never()).buscarPorCorreo(any(), anyBoolean(), any());
    }

    @Test
    void adminSucursalVeLasCuentasAsignablesComoCajero() throws Exception {
        sesion("token", usuario(Set.of(Roles.ADMIN_SUCURSAL), UUID.randomUUID()));
        when(servicio.asignables(PuestoAsignable.PERSONAL)).thenReturn(List.of(
                new UsuarioAsignableResponse(UUID.randomUUID(), "ana@correo.com", "Ana López", null)));

        mvc.perform(get("/identity/usuarios/asignables").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].correo").value("ana@correo.com"));
    }

    @Test
    void adminSucursalNoPuedeListarCandidatosAAdminDelSistema() throws Exception {
        sesion("token", usuario(Set.of(Roles.ADMIN_SUCURSAL), UUID.randomUUID()));

        mvc.perform(get("/identity/usuarios/asignables").param("puesto", "ADMIN_SISTEMA")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
        verify(servicio, never()).asignables(any());
    }

    @Test
    void buscarParaChoferPasaElPuesto() throws Exception {
        sesion("token", usuario(Set.of(Roles.ADMIN_SISTEMA), null));
        when(servicio.buscarPorCorreo("ana@correo.com", true, PuestoAsignable.CHOFER)).thenReturn(
                new UsuarioBusquedaResponse(UUID.randomUUID(), "ana@correo.com", "Ana López", null, false,
                        "La cuenta ya es (o fue) de un chofer"));

        mvc.perform(get("/identity/usuarios/por-correo").param("correo", "ana@correo.com").param("puesto", "CHOFER")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.puedeSerPersonal").value(false));
    }

    @Test
    void hacerAdministradorSistemaDevuelve200() throws Exception {
        // Arrange
        sesion("token", usuario(Set.of(Roles.ADMIN_SISTEMA), null));
        UUID id = UUID.randomUUID();
        when(servicio.hacerAdministradorSistema(id)).thenReturn(detalle(id));

        // Act + Assert
        mvc.perform(post("/identity/usuarios/" + id + "/administrador-sistema")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roles[0]").value(Roles.ADMIN_SISTEMA));
    }

    @Test
    void yaEsAdministradorDevuelve409() throws Exception {
        // Arrange
        sesion("token", usuario(Set.of(Roles.ADMIN_SISTEMA), null));
        UUID id = UUID.randomUUID();
        when(servicio.hacerAdministradorSistema(id))
                .thenThrow(new ConflictoException("YA_ES_ADMIN_SISTEMA", "Ya es administrador"));

        // Act + Assert
        mvc.perform(post("/identity/usuarios/" + id + "/administrador-sistema")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.codigo").value("YA_ES_ADMIN_SISTEMA"));
    }

    @Test
    void adminSucursalNoPuedeHacerAdministradorSistema() throws Exception {
        // Arrange
        sesion("token", usuario(Set.of(Roles.ADMIN_SUCURSAL), UUID.randomUUID()));

        // Act + Assert
        mvc.perform(post("/identity/usuarios/" + UUID.randomUUID() + "/administrador-sistema")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
        verify(servicio, never()).hacerAdministradorSistema(any());
    }

    private UsuarioDetalleResponse detalle(UUID id) {
        return new UsuarioDetalleResponse(id, "ana@correo.com", "Ana", "López", "1234567-8",
                "1234567890101", "55551234", "Xela", null, Set.of(Roles.ADMIN_SISTEMA), null, null,
                "ACTIVO", null);
    }
}
