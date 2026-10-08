package gt.cunoc.proyecto2.identity.controllers.auth;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.controllers.BaseControladorTest;
import gt.cunoc.proyecto2.identity.dtos.auth.RegistroResponse;
import gt.cunoc.proyecto2.identity.dtos.auth.SesionResponse;
import gt.cunoc.proyecto2.identity.services.auth.AuthService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest extends BaseControladorTest {

    @MockitoBean
    private AuthService servicio;

    @Test
    void registroDevuelve201ConUbicacion() throws Exception {
        // Arrange
        var respuesta = new RegistroResponse(UUID.randomUUID(), "ana@correo.com", Set.of(Roles.CLIENTE));
        when(servicio.registrar(any())).thenReturn(respuesta);

        // Act + Assert
        mvc.perform(post("/identity/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"ana@correo.com","contrasena":"Segura#2026","nombres":"Ana",
                                "apellidos":"López","nit":"1234567-8","dpi":"1234567890101",
                                "telefono":"55551234","direccion":"Xela"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.correo").value("ana@correo.com"));
        verify(servicio).registrar(any());
    }

    @Test
    void registroInvalidoDevuelveFormatoComun() throws Exception {
        // Act + Assert
        mvc.perform(post("/identity/auth/registro")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"no-es-correo","contrasena":"x"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"))
                .andExpect(jsonPath("$.errores").isArray());
    }

    @Test
    void loginDevuelveSesion() throws Exception {
        // Arrange
        when(servicio.iniciarSesion(any())).thenReturn(sesion());

        // Act + Assert
        mvc.perform(post("/identity/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"ana@correo.com","contrasena":"Segura#2026"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenAcceso").value("jwt-test"));
    }

    @Test
    void cerrarSesionExigeToken() throws Exception {
        // Act + Assert
        mvc.perform(post("/identity/auth/cerrar-sesion")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tokenRefresco":"x"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.correlationId").exists());
    }

    @Test
    void cambiarDobleFactorConSesionDevuelve204() throws Exception {
        // Arrange
        var actual = usuario(Set.of(Roles.CLIENTE), null);
        sesion("buen-token", actual);

        // Act + Assert
        mvc.perform(put("/identity/auth/doble-factor")
                        .header("Authorization", "Bearer buen-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"activado":true}"""))
                .andExpect(status().isNoContent());
        verify(servicio).cambiarDobleFactor(eq(actual.usuarioId()), any());
    }

    @Test
    void refrescarDevuelveSesion() throws Exception {
        // Arrange
        when(servicio.renovarSesion(any())).thenReturn(sesion());

        // Act + Assert
        mvc.perform(post("/identity/auth/refrescar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tokenRefresco":"crudo"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenAcceso").value("jwt-test"));
    }

    @Test
    void recuperarDevuelve202() throws Exception {
        // Act + Assert
        mvc.perform(post("/identity/auth/recuperar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"ana@correo.com"}"""))
                .andExpect(status().isAccepted());
        verify(servicio).solicitarRecuperacion(any());
    }

    @Test
    void restablecerDevuelve204() throws Exception {
        // Act + Assert
        mvc.perform(post("/identity/auth/restablecer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"abc","nuevaContrasena":"Nueva#2026"}"""))
                .andExpect(status().isNoContent());
    }

    @Test
    void cambiarContrasenaDevuelve204() throws Exception {
        // Arrange
        var actual = usuario(Set.of(Roles.CLIENTE), null);
        sesion("buen-token", actual);

        // Act + Assert
        mvc.perform(put("/identity/auth/contrasena")
                        .header("Authorization", "Bearer buen-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"contrasenaActual":"Segura#2026","nuevaContrasena":"Nueva#2026"}"""))
                .andExpect(status().isNoContent());
    }

    @Test
    void verificarDobleFactorDevuelveSesion() throws Exception {
        // Arrange
        when(servicio.verificarDobleFactor(any())).thenReturn(sesion());

        // Act + Assert
        mvc.perform(post("/identity/auth/login/verificar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"correo":"ana@correo.com","codigo":"123456"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipo").value("Bearer"));
    }

    private SesionResponse sesion() {
        return new SesionResponse("jwt-test", "Bearer", "2026-10-08T08:00:00Z",
                "refresco", UUID.randomUUID(), null);
    }
}
