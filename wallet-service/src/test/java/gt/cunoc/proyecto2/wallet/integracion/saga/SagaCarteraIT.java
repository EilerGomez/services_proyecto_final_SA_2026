package gt.cunoc.proyecto2.wallet.integracion.saga;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.wallet.integracion.soporte.PruebaIntegracion;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Saga de cartera con Sales: débito, rechazos y reembolso por eventos (sales.eventos → wallet.eventos). */
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
}
