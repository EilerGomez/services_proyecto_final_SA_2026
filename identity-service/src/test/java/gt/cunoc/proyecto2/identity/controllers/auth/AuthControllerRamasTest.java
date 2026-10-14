package gt.cunoc.proyecto2.identity.controllers.auth;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.controllers.BaseControladorTest;
import gt.cunoc.proyecto2.identity.dtos.auth.DesafioDobleFactor;
import gt.cunoc.proyecto2.identity.services.auth.AuthService;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Ramas de AuthController sin cubrir: desafío 2FA (202) y cierre de sesión (204). */
@WebMvcTest(AuthController.class)
class AuthControllerRamasTest extends BaseControladorTest {

    @MockitoBean
    private AuthService servicio;

    @Test
    void loginConDobleFactorDevuelve202() throws Exception {
        // Arrange
        when(servicio.iniciarSesion(any())).thenReturn(
                new DesafioDobleFactor(UUID.randomUUID(), "ana@correo.com", Instant.now()));

        // Act + Assert
        mvc.perform(post("/identity/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"ana@correo.com","contrasena":"Segura#2026"}"""))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.correo").value("ana@correo.com"));
    }

    @Test
    void cerrarSesionConSesionDevuelve204() throws Exception {
        // Arrange
        var actual = usuario(Set.of(Roles.CLIENTE), null);
        sesion("buen-token", actual);

        // Act + Assert
        mvc.perform(post("/identity/auth/cerrar-sesion")
                        .header("Authorization", "Bearer buen-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tokenRefresco":"crudo"}"""))
                .andExpect(status().isNoContent());
        verify(servicio).cerrarSesion(eq(actual.usuarioId()), any());
    }
}
