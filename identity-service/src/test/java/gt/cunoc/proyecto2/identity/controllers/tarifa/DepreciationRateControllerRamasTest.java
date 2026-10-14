package gt.cunoc.proyecto2.identity.controllers.tarifa;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.controllers.BaseControladorTest;
import gt.cunoc.proyecto2.identity.dtos.tarifa.DepreciationRateResponse;
import gt.cunoc.proyecto2.identity.services.tarifa.TarifaService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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

/** Rama de DepreciationRateController sin cubrir: alta exitosa (201). */
@WebMvcTest(DepreciationRateController.class)
class DepreciationRateControllerRamasTest extends BaseControladorTest {

    @MockitoBean
    private TarifaService servicio;

    @Test
    void crearComoAdminDevuelve201() throws Exception {
        // Arrange
        var actual = usuario(Set.of(Roles.ADMIN_SISTEMA), null);
        sesion("token", actual);
        var tarifa = new DepreciationRateResponse(UUID.randomUUID(), new BigDecimal("1.2500"),
                LocalDate.of(2026, 10, 10), actual.usuarioId(), Instant.now());
        when(servicio.crear(any(), eq(actual.usuarioId()))).thenReturn(tarifa);

        // Act + Assert
        mvc.perform(post("/identity/tarifas-depreciacion")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"montoPorKm":1.25,"vigenteDesde":"2026-10-10"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tarifaId").value(tarifa.tarifaId().toString()));
        verify(servicio).crear(any(), eq(actual.usuarioId()));
    }
}
