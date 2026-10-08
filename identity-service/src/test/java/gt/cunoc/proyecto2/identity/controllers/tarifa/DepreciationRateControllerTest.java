package gt.cunoc.proyecto2.identity.controllers.tarifa;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.controllers.BaseControladorTest;
import gt.cunoc.proyecto2.identity.dtos.tarifa.DepreciationRateResponse;
import gt.cunoc.proyecto2.identity.services.tarifa.TarifaService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DepreciationRateController.class)
class DepreciationRateControllerTest extends BaseControladorTest {

    @MockitoBean
    private TarifaService servicio;

    @Test
    void historialDevuelveArreglo() throws Exception {
        // Arrange
        sesion("token", usuario(Set.of(Roles.ADMIN_SISTEMA), null));
        when(servicio.listar(any())).thenReturn(new PageImpl<>(List.of(tarifa())));

        // Act + Assert
        mvc.perform(get("/identity/tarifas-depreciacion")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].montoPorKm").value(1.25));
    }

    @Test
    void vigentePorFechaDevuelveUna() throws Exception {
        // Arrange
        sesion("token", usuario(Set.of(Roles.ADMIN_SISTEMA), null));
        when(servicio.vigente(LocalDate.of(2026, 10, 20))).thenReturn(tarifa());

        // Act + Assert
        mvc.perform(get("/identity/tarifas-depreciacion?vigenteEn=2026-10-20")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tarifaId").exists());
    }

    @Test
    void crearExigeAdminSistema() throws Exception {
        // Arrange
        sesion("token", usuario(Set.of(Roles.CLIENTE), null));

        // Act + Assert
        mvc.perform(post("/identity/tarifas-depreciacion")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montoPorKm":1.25,"vigenteDesde":"2026-10-10"}"""))
                .andExpect(status().isForbidden());
    }

    private DepreciationRateResponse tarifa() {
        return new DepreciationRateResponse(UUID.randomUUID(), new BigDecimal("1.2500"),
                LocalDate.of(2026, 10, 10), UUID.randomUUID(), Instant.now());
    }
}
