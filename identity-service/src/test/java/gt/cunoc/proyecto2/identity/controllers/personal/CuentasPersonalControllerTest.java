package gt.cunoc.proyecto2.identity.controllers.personal;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.controllers.BaseControladorTest;
import gt.cunoc.proyecto2.identity.dtos.personal.CuentaPersonalResponse;
import gt.cunoc.proyecto2.identity.services.personal.CuentasPersonalService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CuentasPersonalController.class)
class CuentasPersonalControllerTest extends BaseControladorTest {

    @MockitoBean
    private CuentasPersonalService servicio;

    @Test
    void cuentaNuevaDevuelve201() throws Exception {
        // Arrange
        var actual = usuario(java.util.Set.of(Roles.SERVICIO), null);
        sesion("token", actual);
        when(servicio.crear(any())).thenReturn(
                new CuentaPersonalResponse(UUID.randomUUID(), "chofer@correo.com", "Pedro Ramírez", true));

        // Act + Assert
        mvc.perform(post("/identity/interno/cuentas-personal")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.creada").value(true));
    }

    @Test
    void reintentoDevuelve200() throws Exception {
        // Arrange
        var actual = usuario(java.util.Set.of(Roles.SERVICIO), null);
        sesion("token", actual);
        when(servicio.crear(any())).thenReturn(
                new CuentaPersonalResponse(UUID.randomUUID(), "chofer@correo.com", "Pedro Ramírez", false));

        // Act + Assert
        mvc.perform(post("/identity/interno/cuentas-personal")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creada").value(false));
    }

    @Test
    void sinRolServicioDa403() throws Exception {
        // Arrange
        sesion("token", usuario(java.util.Set.of(Roles.CLIENTE), null));

        // Act + Assert
        mvc.perform(post("/identity/interno/cuentas-personal")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body()))
                .andExpect(status().isForbidden());
    }

    private String body() {
        return """
                {"solicitudId":"%s","rol":"CHOFER","sucursalId":"%s",
                "choferId":"%s","correo":"chofer@correo.com","nombres":"Pedro",
                "apellidos":"Ramírez","nit":"9988776-5","dpi":"3234567890101",
                "telefono":"55553333","direccion":"Xela",
                "fotoUrl":"https://stproyecto2fotos.blob.core.windows.net/fotos/choferes/a.jpg"}"""
                .formatted(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
    }
}
