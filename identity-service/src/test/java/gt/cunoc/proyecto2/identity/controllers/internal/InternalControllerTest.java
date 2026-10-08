package gt.cunoc.proyecto2.identity.controllers.internal;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.controllers.BaseControladorTest;
import gt.cunoc.proyecto2.identity.dtos.usuario.AdminContactoResponse;
import gt.cunoc.proyecto2.identity.dtos.usuario.ContactoResponse;
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
}
