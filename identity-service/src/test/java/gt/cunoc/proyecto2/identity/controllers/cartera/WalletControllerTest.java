package gt.cunoc.proyecto2.identity.controllers.cartera;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.identity.controllers.BaseControladorTest;
import gt.cunoc.proyecto2.identity.dtos.cartera.WalletMovementResponse;
import gt.cunoc.proyecto2.identity.dtos.cartera.WalletResponse;
import gt.cunoc.proyecto2.identity.services.cartera.WalletService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
class WalletControllerTest extends BaseControladorTest {

    @MockitoBean
    private WalletService servicio;

    @Test
    void saldoSinTokenDa401() throws Exception {
        // Act + Assert
        mvc.perform(get("/identity/cartera"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void saldoDevuelveSaldo() throws Exception {
        // Arrange
        var actual = usuario(Set.of(Roles.CLIENTE), null);
        sesion("token", actual);
        when(servicio.saldo(actual.usuarioId()))
                .thenReturn(new WalletResponse(UUID.randomUUID(), new BigDecimal("350.00"), Instant.now()));

        // Act + Assert
        mvc.perform(get("/identity/cartera").header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.saldo").value(350.00));
    }

    @Test
    void recargaDevuelve201() throws Exception {
        // Arrange
        var actual = usuario(Set.of(Roles.CLIENTE), null);
        sesion("token", actual);
        when(servicio.recargar(eq(actual.usuarioId()), any())).thenReturn(movimiento());

        // Act + Assert
        mvc.perform(post("/identity/cartera/recargas")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto":200.00,"fechaMovimiento":"2026-10-20"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("RECARGA"));
    }

    @Test
    void recargaMontoCeroDa400() throws Exception {
        // Arrange
        sesion("token", usuario(Set.of(Roles.CLIENTE), null));

        // Act + Assert
        mvc.perform(post("/identity/cartera/recargas")
                        .header("Authorization", "Bearer token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"monto":0,"fechaMovimiento":"2026-10-20"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("VALIDACION"));
    }

    @Test
    void movimientosAceptaFiltrosDeFecha() throws Exception {
        // Arrange
        var actual = usuario(Set.of(Roles.CLIENTE), null);
        sesion("token", actual);
        when(servicio.movimientos(eq(actual.usuarioId()), any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(movimiento())));

        // Act + Assert
        mvc.perform(get("/identity/cartera/movimientos?desde=2026-10-01&hasta=2026-10-31")
                        .header("Authorization", "Bearer token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tipo").value("RECARGA"));
    }

    private WalletMovementResponse movimiento() {
        return new WalletMovementResponse(UUID.randomUUID(), "RECARGA", new BigDecimal("200.00"),
                new BigDecimal("550.00"), "MANUAL", null, LocalDate.of(2026, 10, 20));
    }
}
