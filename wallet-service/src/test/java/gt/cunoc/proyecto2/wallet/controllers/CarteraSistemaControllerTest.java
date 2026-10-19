package gt.cunoc.proyecto2.wallet.controllers;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.wallet.dtos.CarteraSistemaResponse;
import gt.cunoc.proyecto2.wallet.dtos.WalletMovementResponse;
import gt.cunoc.proyecto2.wallet.services.CarteraSistemaService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CarteraSistemaController.class)
class CarteraSistemaControllerTest extends BaseControladorTest {

    @MockitoBean
    private CarteraSistemaService servicio;

    @Test
    void unClienteNoVeLaBilleteraDelSistema() throws Exception {
        sesion("token", usuario(Set.of(Roles.CLIENTE)));

        mvc.perform(get("/wallet/sistema").header("Authorization", "Bearer token"))
                .andExpect(status().isForbidden());
        verifyNoInteractions(servicio);
    }

    @Test
    void elAdminDelSistemaVeSaldoYTotales() throws Exception {
        sesion("token", usuario(Set.of(Roles.ADMIN_SISTEMA)));
        when(servicio.saldo()).thenReturn(new CarteraSistemaResponse(UUID.randomUUID(), new BigDecimal("100.00"),
                new BigDecimal("150.00"), new BigDecimal("50.00"), Instant.now()));

        mvc.perform(get("/wallet/sistema").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(100.00))
                .andExpect(jsonPath("$.totalCobrado").value(150.00))
                .andExpect(jsonPath("$.totalDevuelto").value(50.00));
    }

    @Test
    void movimientosConFiltros() throws Exception {
        sesion("token", usuario(Set.of(Roles.ADMIN_SISTEMA)));
        when(servicio.movimientos(eq("COBRO"), eq(LocalDate.of(2026, 10, 1)), eq(LocalDate.of(2026, 10, 31)), any()))
                .thenReturn(new PageImpl<>(List.of(new WalletMovementResponse(UUID.randomUUID(), "COBRO",
                        new BigDecimal("40.00"), new BigDecimal("140.00"), "VENTA", UUID.randomUUID(),
                        LocalDate.of(2026, 10, 20)))));

        mvc.perform(get("/wallet/sistema/movimientos")
                        .param("tipo", "COBRO").param("desde", "2026-10-01").param("hasta", "2026-10-31")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("COBRO"))
                .andExpect(jsonPath("$[0].tipoReferencia").value("VENTA"));
    }
}
