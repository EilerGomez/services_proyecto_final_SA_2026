package gt.cunoc.proyecto2.wallet.integracion.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.wallet.integracion.soporte.PruebaIntegracion;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.http.ResponseEntity;

/**
 * La saga de cartera cuando el mensaje llega repetido o viene mal.
 *
 * <p>Sales reintenta: el mismo débito o el mismo reembolso puede llegar dos veces (con otro
 * eventId, así que la idempotencia de {@code eventos_procesados} no lo atrapa). La que manda es
 * entonces {@code UNIQUE (tipo, referencia_id)} de movimientos_cartera: el dinero se mueve una sola
 * vez y la respuesta de la saga se vuelve a publicar.
 *
 * <p>Y cuando el reembolso no se puede atender (sin débito previo, con monto inválido o mayor que
 * la billetera del sistema), nada queda a medias: o no se hace nada, o la transacción se deshace y
 * el evento termina en la DLQ para revisarlo a mano.
 */
class SagaCarteraIdempotenciaIT extends PruebaIntegracion {

    private static final Duration ESPERA = Duration.ofSeconds(15);

    private void solicitarDebito(UUID usuarioId, UUID referencia, String monto) {
        publicarEvento(SALES_EVENTOS, "DebitoCarteraSolicitado", referencia, """
                {"referenciaTipo":"VENTA","referenciaId":"%s","usuarioId":"%s",
                 "monto":%s,"fechaMovimiento":"%s"}
                """.formatted(referencia, usuarioId, monto, FECHA));
    }

    private void solicitarReembolso(UUID usuarioId, UUID referencia, String monto) {
        publicarEvento(SALES_EVENTOS, "ReembolsoCarteraSolicitado", referencia, """
                {"referenciaTipo":"VENTA","referenciaId":"%s","usuarioId":"%s",
                 "monto":%s,"motivo":"CANCELACION","fechaMovimiento":"%s"}
                """.formatted(referencia, usuarioId, monto, FECHA));
    }

    /** Compra pagada con cartera: deja el DEBITO del cliente y el COBRO de la billetera del sistema. */
    private void comprar(UUID usuarioId, UUID referencia, String monto) {
        solicitarDebito(usuarioId, referencia, monto);
        esperarEventos("CarteraDebitada");
    }

    private int movimientos(String tipo) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM movimientos_cartera WHERE tipo = ?",
                Integer.class, tipo);
        return total == null ? 0 : total;
    }

    private BigDecimal saldoDe(UUID usuarioId) {
        return jdbc.queryForObject("SELECT saldo FROM carteras WHERE usuario_id = ?", BigDecimal.class, usuarioId);
    }

    private BigDecimal saldoDelSistema() {
        return jdbc.queryForObject("SELECT saldo FROM carteras WHERE tipo = 'SISTEMA'", BigDecimal.class);
    }

    private ResponseEntity<String> billeteraSistema() {
        return get("/wallet/sistema", jwt.generar(new UsuarioActual(UUID.randomUUID(), "admin@pruebas.gt",
                Set.of(Roles.ADMIN_SISTEMA), null, null)));
    }

    /** Mensajes que terminaron en la DLQ de Wallet, esperando hasta que lleguen los que se esperan. */
    private List<String> enLaDlq(int esperados) {
        List<String> mensajes = new ArrayList<>();
        long limite = System.currentTimeMillis() + ESPERA.toMillis();
        while (mensajes.size() < esperados && System.currentTimeMillis() < limite) {
            Message mensaje = rabbit.receive(DLQ, 500);
            if (mensaje != null) {
                mensajes.add(new String(mensaje.getBody(), StandardCharsets.UTF_8));
            }
        }
        assertThat(mensajes).as("mensajes en la DLQ de Wallet").hasSize(esperados);
        return mensajes;
    }

    // ----- Repetidos -----

    @Test
    void elMismoDebitoDosVecesDescuentaUnaVezYVuelveAResponderLaSaga() {
        UUID usuarioId = UUID.randomUUID();
        UUID compra = UUID.randomUUID();
        recargar(usuarioId, "500.00");

        comprar(usuarioId, compra, "150.00");
        solicitarDebito(usuarioId, compra, "150.00");   // otro eventId, la misma venta

        // Sales recibe su CarteraDebitada otra vez: la saga no se queda esperando
        assertThat(esperarEventos("CarteraDebitada").get("CarteraDebitada")).contains("\"saldoResultante\": 350.00");
        assertThat(movimientos("DEBITO")).isEqualTo(1);
        assertThat(movimientos("COBRO")).isEqualTo(1);
        assertThat(saldoDe(usuarioId)).isEqualByComparingTo("350.00");
        assertThat(saldoDelSistema()).isEqualByComparingTo("150.00");
    }

    @Test
    void elMismoReembolsoDosVecesAcreditaUnaVezYVuelveAResponderLaSaga() {
        UUID usuarioId = UUID.randomUUID();
        UUID compra = UUID.randomUUID();
        recargar(usuarioId, "500.00");
        comprar(usuarioId, compra, "150.00");

        solicitarReembolso(usuarioId, compra, "150.00");
        esperarEventos("CarteraReembolsada");
        solicitarReembolso(usuarioId, compra, "150.00");   // otro eventId, el mismo reembolso

        assertThat(esperarEventos("CarteraReembolsada").get("CarteraReembolsada"))
                .contains("\"saldoResultante\": 500.00");
        assertThat(movimientos("REEMBOLSO")).isEqualTo(1);
        assertThat(movimientos("DEVOLUCION")).isEqualTo(1);
        assertThat(saldoDe(usuarioId)).isEqualByComparingTo("500.00");
        assertThat(saldoDelSistema()).isEqualByComparingTo("0.00");
    }

    // ----- Reembolsos que no se pueden atender -----

    @Test
    void unReembolsoSinDebitoPrevioNoAcreditaNada() {
        UUID usuarioId = UUID.randomUUID();
        recargar(usuarioId, "500.00");

        solicitarReembolso(usuarioId, UUID.randomUUID(), "150.00");

        await().atMost(ESPERA).until(() -> contar("eventos_procesados") == 1);
        assertThat(movimientos("REEMBOLSO")).isZero();
        assertThat(saldoDe(usuarioId)).isEqualByComparingTo("500.00");
    }

    @Test
    void unaCompraQueNuncaEntroALaBilleteraDelSistemaSeReembolsaSinDescontarla() {
        UUID usuarioId = UUID.randomUUID();
        UUID compra = UUID.randomUUID();
        recargar(usuarioId, "500.00");
        comprar(usuarioId, compra, "150.00");

        // Una venta vieja, de antes de que existiera la billetera del sistema: tiene DEBITO pero no COBRO
        jdbc.update("UPDATE movimientos_cartera SET referencia_id = ? WHERE tipo = 'COBRO'", UUID.randomUUID());

        solicitarReembolso(usuarioId, compra, "150.00");

        // Al cliente se le devuelve igual; de la billetera del sistema no sale nada
        assertThat(esperarEventos("CarteraReembolsada")).isNotEmpty();
        assertThat(saldoDe(usuarioId)).isEqualByComparingTo("500.00");
        assertThat(movimientos("DEVOLUCION")).isZero();
        assertThat(saldoDelSistema()).isEqualByComparingTo("150.00");
        assertThat(PruebaIntegracion.<Number>json(billeteraSistema(), "$.totalDevuelto").doubleValue()).isZero();
    }

    @Test
    void unReembolsoConMontoInvalidoSeDeshaceYCaeALaDLQ() {
        UUID usuarioId = UUID.randomUUID();
        UUID compra = UUID.randomUUID();
        recargar(usuarioId, "500.00");
        comprar(usuarioId, compra, "150.00");

        solicitarReembolso(usuarioId, compra, "0");      // monto en cero
        solicitarReembolso(usuarioId, compra, "null");   // y sin monto

        assertThat(enLaDlq(2)).allMatch(mensaje -> mensaje.contains("ReembolsoCarteraSolicitado"));
        assertThat(movimientos("REEMBOLSO")).isZero();
        assertThat(saldoDe(usuarioId)).isEqualByComparingTo("350.00");
    }

    @Test
    void unReembolsoMayorQueLaBilleteraDelSistemaSeDeshaceYNoDejaSaldoNegativo() {
        UUID usuarioId = UUID.randomUUID();
        UUID compra = UUID.randomUUID();
        recargar(usuarioId, "500.00");
        comprar(usuarioId, compra, "150.00");

        // La billetera del sistema solo recibió 150: devolver 300 la dejaría negativa
        solicitarReembolso(usuarioId, compra, "300.00");

        assertThat(enLaDlq(1)).allMatch(mensaje -> mensaje.contains("ReembolsoCarteraSolicitado"));
        assertThat(movimientos("REEMBOLSO")).isZero();
        assertThat(saldoDe(usuarioId)).isEqualByComparingTo("350.00");
        assertThat(saldoDelSistema()).isEqualByComparingTo("150.00");
    }
}
