package gt.cunoc.proyecto2.wallet.integracion.saga;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.wallet.integracion.soporte.PruebaIntegracion;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.junit.jupiter.api.Test;

/**
 * Saga de cartera con Sales: débito, rechazos y reembolso por eventos (sales.eventos → wallet.eventos).
 * Cada débito entra a la billetera del sistema (COBRO) y cada reembolso sale de ella (DEVOLUCION).
 */
class SagaCarteraIT extends PruebaIntegracion {

    private void solicitarDebito(UUID usuarioId, UUID referencia, String monto) {
        publicarEvento(SALES_EVENTOS, "DebitoCarteraSolicitado", referencia, """
                {"referenciaTipo":"VENTA","referenciaId":"%s","usuarioId":"%s",
                 "monto":%s,"fechaMovimiento":"2026-10-20"}
                """.formatted(referencia, usuarioId, monto));
    }

    @Test
    void debitoConSaldoPublicaCarteraDebitada() {
        UUID usuarioId = UUID.randomUUID();
        recargar(usuarioId, "500.00");
        UUID referencia = UUID.randomUUID();

        solicitarDebito(usuarioId, referencia, "150.00");

        Map<String, String> eventos = esperarEventos("CarteraDebitada");
        assertThat(eventos.get("CarteraDebitada")).contains("\"saldoResultante\": 350.00");
    }

    @Test
    void debitoSinSaldoPublicaRechazo() {
        UUID usuarioId = UUID.randomUUID();
        recargar(usuarioId, "10.00");
        UUID referencia = UUID.randomUUID();

        solicitarDebito(usuarioId, referencia, "150.00");

        Map<String, String> eventos = esperarEventos("DebitoCarteraRechazado");
        assertThat(eventos.get("DebitoCarteraRechazado")).contains("SALDO_INSUFICIENTE");
    }

    @Test
    void debitoSinCarteraPublicaRechazo() {
        solicitarDebito(UUID.randomUUID(), UUID.randomUUID(), "150.00");

        Map<String, String> eventos = esperarEventos("DebitoCarteraRechazado");
        assertThat(eventos.get("DebitoCarteraRechazado")).contains("CARTERA_INEXISTENTE");
    }

    @Test
    void debitoDeCuentaBloqueadaEnIdentityPublicaRechazo() {
        UUID usuarioId = UUID.randomUUID();
        recargar(usuarioId, "500.00");
        estadoEnIdentity(usuarioId, "BLOQUEADO");

        solicitarDebito(usuarioId, UUID.randomUUID(), "150.00");

        Map<String, String> eventos = esperarEventos("DebitoCarteraRechazado");
        assertThat(eventos.get("DebitoCarteraRechazado")).contains("USUARIO_INACTIVO");
    }

    @Test
    void reembolsoAcreditaYPublica() {
        UUID usuarioId = UUID.randomUUID();
        recargar(usuarioId, "500.00");
        UUID referencia = UUID.randomUUID();
        solicitarDebito(usuarioId, referencia, "150.00");
        esperarEventos("CarteraDebitada");

        publicarEvento(SALES_EVENTOS, "ReembolsoCarteraSolicitado", referencia, """
                {"referenciaTipo":"VENTA","referenciaId":"%s","usuarioId":"%s",
                 "monto":150.00,"motivo":"CANCELACION","fechaMovimiento":"2026-10-20"}
                """.formatted(referencia, usuarioId));

        Map<String, String> eventos = esperarEventos("CarteraReembolsada");
        assertThat(eventos.get("CarteraReembolsada")).contains("\"saldoResultante\": 500.00");
    }

    // ----- Billetera del sistema -----

    private String tokenAdminSistema() {
        return jwt.generar(new UsuarioActual(UUID.randomUUID(), "admin@pruebas.gt", Set.of(Roles.ADMIN_SISTEMA),
                null, null));
    }

    private ResponseEntity<String> billeteraSistema() {
        ResponseEntity<String> respuesta = get("/wallet/sistema", tokenAdminSistema());
        assertThat(respuesta.getStatusCode().value()).as(respuesta.getBody()).isEqualTo(200);
        return respuesta;
    }

    @Test
    void laBilleteraDelSistemaRecibeLasComprasYPagaLosReembolsos() {
        UUID usuarioId = UUID.randomUUID();
        recargar(usuarioId, "500.00");
        assertThat(PruebaIntegracion.<Number>json(billeteraSistema(), "$.saldo").doubleValue()).isZero();

        UUID compra = UUID.randomUUID();
        UUID otraCompra = UUID.randomUUID();
        solicitarDebito(usuarioId, compra, "150.00");
        esperarEventos("CarteraDebitada");
        solicitarDebito(usuarioId, otraCompra, "40.00");
        esperarEventos("CarteraDebitada");

        ResponseEntity<String> trasCompras = billeteraSistema();
        assertThat(PruebaIntegracion.<Number>json(trasCompras, "$.saldo").doubleValue()).isEqualTo(190.00);
        assertThat(PruebaIntegracion.<Number>json(trasCompras, "$.totalCobrado").doubleValue()).isEqualTo(190.00);

        publicarEvento(SALES_EVENTOS, "ReembolsoCarteraSolicitado", compra, """
                {"referenciaTipo":"VENTA","referenciaId":"%s","usuarioId":"%s",
                 "monto":150.00,"motivo":"CANCELACION","fechaMovimiento":"2026-10-20"}
                """.formatted(compra, usuarioId));
        esperarEventos("CarteraReembolsada");

        ResponseEntity<String> trasReembolso = billeteraSistema();
        assertThat(PruebaIntegracion.<Number>json(trasReembolso, "$.saldo").doubleValue()).isEqualTo(40.00);
        assertThat(PruebaIntegracion.<Number>json(trasReembolso, "$.totalDevuelto").doubleValue()).isEqualTo(150.00);

        ResponseEntity<String> devoluciones =
                get("/wallet/sistema/movimientos?tipo=DEVOLUCION", tokenAdminSistema());
        assertThat(PruebaIntegracion.<List<String>>json(devoluciones, "$[*].referenciaId"))
                .containsExactly(compra.toString());
        // La cartera del cliente: 500 - 150 - 40 + 150
        assertThat(PruebaIntegracion.<Number>json(get("/wallet/cartera", tokenCliente(usuarioId)), "$.saldo")
                .doubleValue()).isEqualTo(460.00);
    }

    @Test
    void unRechazoNoMueveLaBilleteraDelSistema() {
        UUID usuarioId = UUID.randomUUID();
        recargar(usuarioId, "10.00");

        solicitarDebito(usuarioId, UUID.randomUUID(), "150.00");
        esperarEventos("DebitoCarteraRechazado");

        assertThat(PruebaIntegracion.<Number>json(billeteraSistema(), "$.saldo").doubleValue()).isZero();
    }

    @Test
    void unClienteNoVeLaBilleteraDelSistema() {
        esError(get("/wallet/sistema", tokenCliente(UUID.randomUUID())), 403, "ROL_INSUFICIENTE");
    }
}
