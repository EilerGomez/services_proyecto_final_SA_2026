package gt.cunoc.proyecto2.identity.controllers.personal;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.controllers.BaseControladorTest;
import gt.cunoc.proyecto2.identity.dtos.personal.CuentaPersonalResponse;
import gt.cunoc.proyecto2.identity.dtos.personal.VincularCuentaRequest;
import gt.cunoc.proyecto2.identity.services.personal.CuentasPersonalService;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** R5b · POST /identity/interno/cuentas-personal/existente */
@WebMvcTest(CuentasPersonalController.class)
class VincularCuentaControllerTest extends BaseControladorTest {

    private static final String RUTA = "/identity/interno/cuentas-personal/existente";

    @MockitoBean
    private CuentasPersonalService servicio;

    @Test
    void cuentaExistenteDevuelve200ConLosDatosDeLaCuenta() throws Exception {
        // Arrange
        sesion("token", usuario(Set.of(Roles.SERVICIO), null));
        UUID usuarioId = UUID.randomUUID();
        when(servicio.vincular(any())).thenReturn(
                new CuentaPersonalResponse(usuarioId, "ana@correo.com", "Ana López", false));

        // Act + Assert
        mvc.perform(post(RUTA).header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ADMIN_SUCURSAL")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuarioId").value(usuarioId.toString()))
                .andExpect(jsonPath("$.creada").value(false));
        ArgumentCaptor<VincularCuentaRequest> solicitud = ArgumentCaptor.forClass(VincularCuentaRequest.class);
        verify(servicio).vincular(solicitud.capture());
        assertThat(solicitud.getValue().sucursalNombre()).isEqualTo("Xela Central");
    }

    @Test
    void choferSePuedeVincular() throws Exception {
        // Arrange: ahora un chofer también puede darse de alta con una cuenta existente
        sesion("token", usuario(Set.of(Roles.SERVICIO), null));
        UUID usuarioId = UUID.randomUUID();
        when(servicio.vincular(any())).thenReturn(
                new CuentaPersonalResponse(usuarioId, "ana@correo.com", "Ana López", false));

        // Act + Assert
        mvc.perform(post(RUTA).header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("CHOFER")))
                .andExpect(status().isOk());
        ArgumentCaptor<VincularCuentaRequest> solicitud = ArgumentCaptor.forClass(VincularCuentaRequest.class);
        verify(servicio).vincular(solicitud.capture());
        assertThat(solicitud.getValue().rol()).isEqualTo(Roles.CHOFER);
    }

    @Test
    void rolQueNoEsDePersonalNiChoferDa400() throws Exception {
        // Arrange
        sesion("token", usuario(Set.of(Roles.SERVICIO), null));

        // Act + Assert
        mvc.perform(post(RUTA).header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("ADMIN_SISTEMA")))
                .andExpect(status().isBadRequest());
        verify(servicio, never()).vincular(any());
    }

    @Test
    void sinRolServicioDa403() throws Exception {
        // Arrange
        sesion("token", usuario(Set.of(Roles.ADMIN_SISTEMA), null));

        // Act + Assert
        mvc.perform(post(RUTA).header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("CAJERO")))
                .andExpect(status().isForbidden());
    }

    private String body(String rol) {
        return """
                {"solicitudId":"%s","usuarioId":"%s","rol":"%s","sucursalId":"%s","sucursalNombre":"Xela Central"}"""
                .formatted(UUID.randomUUID(), UUID.randomUUID(), rol, UUID.randomUUID());
    }
}
