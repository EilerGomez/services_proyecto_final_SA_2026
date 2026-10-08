package gt.cunoc.proyecto2.identity.integracion.saga;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.identity.integracion.soporte.PruebaIntegracion;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Saga de cartera con Sales: débito, rechazo y reembolso por eventos. */
class SagaCarteraIT extends PruebaIntegracion {

    @Test
    void debitoConSaldoPublicaCarteraDebitada() {
        UUID usuarioId = registrarCliente("ana@pruebas.gt", "1000000-1", "1000000000101");
        String token = tokenCliente(usuarioId, "ana@pruebas.gt");
        post("/identity/cartera/recargas", token,
                "{\"monto\":500.00,\"fechaMovimiento\":\"2026-10-20\"}");
        UUID referencia = UUID.randomUUID();

        publicarEvento("sales.eventos", "DebitoCarteraSolicitado", referencia, """
                {"referenciaTipo":"VENTA","referenciaId":"%s","usuarioId":"%s",
                 "monto":150.00,"fechaMovimiento":"2026-10-20"}
                """.formatted(referencia, usuarioId));

        Map<String, String> eventos = esperarEventos("CarteraDebitada");
        assertThat(eventos.get("CarteraDebitada")).contains("\"saldoResultante\": 350.00");
    }

    @Test
    void debitoSinSaldoPublicaRechazo() {
        UUID usuarioId = registrarCliente("ana@pruebas.gt", "1000000-1", "1000000000101");
        UUID referencia = UUID.randomUUID();

        publicarEvento("sales.eventos", "DebitoCarteraSolicitado", referencia, """
                {"referenciaTipo":"VENTA","referenciaId":"%s","usuarioId":"%s",
                 "monto":150.00,"fechaMovimiento":"2026-10-20"}
                """.formatted(referencia, usuarioId));

        Map<String, String> eventos = esperarEventos("DebitoCarteraRechazado");
        assertThat(eventos.get("DebitoCarteraRechazado")).contains("SALDO_INSUFICIENTE");
    }

    @Test
    void reembolsoAcreditaYPublica() {
        UUID usuarioId = registrarCliente("ana@pruebas.gt", "1000000-1", "1000000000101");
        String token = tokenCliente(usuarioId, "ana@pruebas.gt");
        post("/identity/cartera/recargas", token,
                "{\"monto\":500.00,\"fechaMovimiento\":\"2026-10-20\"}");
        UUID referencia = UUID.randomUUID();
        publicarEvento("sales.eventos", "DebitoCarteraSolicitado", referencia, """
                {"referenciaTipo":"VENTA","referenciaId":"%s","usuarioId":"%s",
                 "monto":150.00,"fechaMovimiento":"2026-10-20"}
                """.formatted(referencia, usuarioId));
        esperarEventos("CarteraDebitada");

        publicarEvento("sales.eventos", "ReembolsoCarteraSolicitado", referencia, """
                {"referenciaTipo":"VENTA","referenciaId":"%s","usuarioId":"%s",
                 "monto":150.00,"motivo":"CANCELACION","fechaMovimiento":"2026-10-20"}
                """.formatted(referencia, usuarioId));

        Map<String, String> eventos = esperarEventos("CarteraReembolsada");
        assertThat(eventos.get("CarteraReembolsada")).contains("\"saldoResultante\": 500.00");
    }
}
