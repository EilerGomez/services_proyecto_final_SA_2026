package gt.cunoc.proyecto2.identity.controllers.perfil;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.controllers.BaseControladorTest;
import gt.cunoc.proyecto2.identity.dtos.usuario.PerfilResponse;
import gt.cunoc.proyecto2.identity.services.usuario.UserService;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PerfilController.class)
class PerfilControllerTest extends BaseControladorTest {

    @MockitoBean
    private UserService servicio;

    @Test
    void verDevuelvePerfil() throws Exception {
        // Arrange
        var actual = usuario(Set.of(Roles.CLIENTE), null);
        sesion("token", actual);
        when(servicio.perfil(actual.usuarioId())).thenReturn(perfil(actual.usuarioId()));

        // Act + Assert
        mvc.perform(get("/identity/perfil").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.correo").value("ana@correo.com"));
    }

    @Test
    void actualizarDevuelvePerfil() throws Exception {
        // Arrange
        var actual = usuario(Set.of(Roles.CLIENTE), null);
        sesion("token", actual);
        when(servicio.actualizarPerfil(eq(actual.usuarioId()), any())).thenReturn(perfil(actual.usuarioId()));

        // Act + Assert
        mvc.perform(put("/identity/perfil")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"telefono":"55559999"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telefono").value("55559999"));
    }

    @Test
    void completarDevuelvePerfil() throws Exception {
        var actual = usuario(Set.of(Roles.CLIENTE), null);
        sesion("token", actual);
        when(servicio.completarPerfil(eq(actual.usuarioId()), any())).thenReturn(perfil(actual.usuarioId()));

        mvc.perform(put("/identity/perfil/completar")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nit":"","dpi":"1234567890101","telefono":"55559999","direccion":"Xela"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dpi").value("1234567890101"));
    }

    @Test
    void completarSinDpiValidoDa400() throws Exception {
        sesion("token", usuario(Set.of(Roles.CLIENTE), null));

        mvc.perform(put("/identity/perfil/completar")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"dpi":"123","telefono":"55559999","direccion":"Xela"}"""))
                .andExpect(status().isBadRequest());
    }

    private PerfilResponse perfil(UUID usuarioId) {
        return new PerfilResponse(usuarioId, "ana@correo.com", "Ana", "López", "1234567-8",
                "1234567890101", "55559999", "Xela", null, Set.of(Roles.CLIENTE), null, null);
    }
}
