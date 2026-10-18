package gt.cunoc.proyecto2.sales.integracion.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.jayway.jsonpath.JsonPath;
import gt.cunoc.proyecto2.sales.integracion.soporte.PruebaIntegracion;
import gt.cunoc.proyecto2.sales.utilidades.Fechas;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Saga del pago de una orden de alquiler con cartera: S12 publica DebitoCarteraSolicitado, la prueba responde
 * como Wallet y se revisa la orden y los eventos (AlquilerPagado, ReembolsoCarteraSolicitado).
 */
class SagaAlquilerIT extends PruebaIntegracion {

    private static final Duration ESPERA = Duration.ofSeconds(15);
    private static final String PAGO = "{\"fechaPago\":\"" + FECHA_PAGO + "\"}";

    private UUID xela;
    private UUID cliente;
    private UUID orden;

    @BeforeEach
    void datos() {
        xela = UUID.randomUUID();
        cliente = UUID.randomUUID();
        orden = insertarOrden(xela, cliente, "1234567-8");          // Q2600, PENDIENTE_PAGO
    }

    // ----- Ayudas -----

    private ResponseEntity<String> pagarConCartera() {
        return post("/sales/ordenes-alquiler/" + orden + "/pago-cartera", tokenCliente(cliente), PAGO);
    }

    private ResponseEntity<String> pagarEnEfectivo() {
        return post("/sales/ordenes-alquiler/" + orden + "/pago-efectivo", tokenCajero(xela), PAGO);
    }

    private UUID carteraDebitada() {
        return publicarEvento(WALLET_EVENTOS, "CarteraDebitada", cliente, null,
                movimientoCartera("ALQUILER", orden, cliente, "2600.00"));
    }

    private UUID responderRechazo(String motivo) {
        return publicarEvento(WALLET_EVENTOS, "DebitoCarteraRechazado", cliente, null,
                debitoRechazado("ALQUILER", orden, cliente, "2600.00", motivo));
    }

    private UUID carteraReembolsada() {
        return publicarEvento(WALLET_EVENTOS, "CarteraReembolsada", cliente, null,
                movimientoCartera("ALQUILER", orden, cliente, "2600.00"));
    }

    private Map<String, Object> filaOrden() {
        return jdbc.queryForMap("SELECT * FROM ordenes_alquiler WHERE id = ?", orden);
    }

    private List<String> enOutbox(String tipo) {
        return jdbc.queryForList("SELECT datos::text FROM eventos_salida WHERE tipo_evento = ? ORDER BY fecha_creacion",
                String.class, tipo);
    }

    private static LocalDate fechaNoAnteriorAlPago() {
        LocalDate hoy = Fechas.hoy();
        return hoy.isBefore(FECHA_PAGO) ? FECHA_PAGO : hoy;
    }

    // ----- Camino feliz -----

    @Test
    void carteraDebitadaPagaLaOrdenYPublicaAlquilerPagado() {
        ResponseEntity<String> respuesta = pagarConCartera();

        esStatus(respuesta, 202);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.estado")).isEqualTo("PENDIENTE_PAGO");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.metodoPago")).isEqualTo("CARTERA");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.fechaPago")).isEqualTo(FECHA_PAGO.toString());

        String debito = esperarEventos("DebitoCarteraSolicitado").get("DebitoCarteraSolicitado");
        assertThat(JsonPath.<String>read(debito, "$.aggregateId")).isEqualTo(orden.toString());
        assertThat(JsonPath.<String>read(debito, "$.businessDate")).isEqualTo(FECHA_PAGO.toString());
        assertThat(JsonPath.<String>read(debito, "$.payload.referenciaTipo")).isEqualTo("ALQUILER");
        assertThat(JsonPath.<String>read(debito, "$.payload.referenciaId")).isEqualTo(orden.toString());
        assertThat(JsonPath.<String>read(debito, "$.payload.usuarioId")).isEqualTo(cliente.toString());
        assertThat(decimal(debito, "$.payload.monto")).isEqualByComparingTo("2600.00");

        // mientras Wallet no responde, otro S12 es 409
        esError(pagarConCartera(), 409, "PAGO_EN_PROCESO");

        carteraDebitada();

        await().atMost(ESPERA).untilAsserted(() -> assertThat(estadoOrden(orden)).isEqualTo("PAGADA"));
        assertThat(filaOrden().get("metodo_pago")).isEqualTo("CARTERA");
        assertThat(filaOrden().get("fecha_pago").toString()).isEqualTo(FECHA_PAGO.toString());

        String pagado = esperarEventos("AlquilerPagado").get("AlquilerPagado");
        UUID alquilerId = (UUID) filaOrden().get("alquiler_id");
        assertThat(JsonPath.<String>read(pagado, "$.aggregateId")).isEqualTo(orden.toString());
        assertThat(JsonPath.<String>read(pagado, "$.businessDate")).isEqualTo(FECHA_PAGO.toString());
        assertThat(JsonPath.<String>read(pagado, "$.payload.ordenId")).isEqualTo(orden.toString());
        assertThat(JsonPath.<String>read(pagado, "$.payload.alquilerId")).isEqualTo(alquilerId.toString());
        assertThat(JsonPath.<String>read(pagado, "$.payload.sucursalId")).isEqualTo(xela.toString());
        assertThat(JsonPath.<String>read(pagado, "$.payload.metodoPago")).isEqualTo("CARTERA");
        assertThat(JsonPath.<String>read(pagado, "$.payload.clienteNit")).isEqualTo("1234567-8");
        assertThat(decimal(pagado, "$.payload.monto")).isEqualByComparingTo("2600.00");
        assertThat(decimal(pagado, "$.payload.montoTotal")).isEqualByComparingTo("2600.00");
        assertThat(JsonPath.<String>read(pagado, "$.payload.fechaPago")).isEqualTo(FECHA_PAGO.toString());
    }

    @Test
    void carteraDebitadaRepetidaNoPublicaOtroAlquilerPagado() {
        esStatus(pagarConCartera(), 202);
        carteraDebitada();
        await().atMost(ESPERA).untilAsserted(() -> assertThat(estadoOrden(orden)).isEqualTo("PAGADA"));

        UUID repetido = carteraDebitada();

        await().atMost(ESPERA).until(() -> procesado(repetido));
        assertThat(estadoOrden(orden)).isEqualTo("PAGADA");
        assertThat(enOutbox("AlquilerPagado")).hasSize(1);
        assertThat(enOutbox("ReembolsoCarteraSolicitado")).isEmpty();
    }

    // ----- Rechazo y reintento -----

    @Test
    void debitoRechazadoRechazaLaOrdenYElClientePuedeReintentar() {
        esStatus(pagarConCartera(), 202);

        responderRechazo("SALDO_INSUFICIENTE");

        await().atMost(ESPERA).untilAsserted(() -> assertThat(estadoOrden(orden)).isEqualTo("RECHAZADA"));
        assertThat(filaOrden().get("motivo_rechazo")).isEqualTo("SALDO_INSUFICIENTE");
        assertThat(enOutbox("AlquilerPagado")).isEmpty();

        // S11 muestra el motivo al cliente
        ResponseEntity<String> detalle = get("/sales/ordenes-alquiler/" + orden, tokenCliente(cliente));
        esStatus(detalle, 200);
        assertThat(PruebaIntegracion.<String>json(detalle, "$.motivoRechazo")).isEqualTo("SALDO_INSUFICIENTE");

        // reintento con S12: vuelve a PENDIENTE_PAGO y se pide otro débito
        ResponseEntity<String> reintento = pagarConCartera();
        esStatus(reintento, 202);
        assertThat(PruebaIntegracion.<String>json(reintento, "$.estado")).isEqualTo("PENDIENTE_PAGO");
        assertThat(PruebaIntegracion.<String>json(reintento, "$.motivoRechazo")).isNull();
        assertThat(enOutbox("DebitoCarteraSolicitado")).hasSize(2);

        carteraDebitada();

        await().atMost(ESPERA).untilAsserted(() -> assertThat(estadoOrden(orden)).isEqualTo("PAGADA"));
        assertThat(filaOrden().get("motivo_rechazo")).isNull();
        esperarEventos("AlquilerPagado");
    }

    @Test
    void respuestasSinCobroEnCursoNoCambianLaOrden() {
        // la orden está PENDIENTE_PAGO pero nadie pidió el cobro con cartera
        UUID rechazo = responderRechazo("SALDO_INSUFICIENTE");
        UUID inexistente = publicarEvento(WALLET_EVENTOS, "DebitoCarteraRechazado", cliente, null,
                debitoRechazado("ALQUILER", UUID.randomUUID(), cliente, "2600.00", "SALDO_INSUFICIENTE"));
        UUID debitoInexistente = publicarEvento(WALLET_EVENTOS, "CarteraDebitada", cliente, null,
                movimientoCartera("ALQUILER", UUID.randomUUID(), cliente, "2600.00"));

        await().atMost(ESPERA)
                .until(() -> procesado(rechazo) && procesado(inexistente) && procesado(debitoInexistente));
        assertThat(estadoOrden(orden)).isEqualTo("PENDIENTE_PAGO");
        assertThat(filaOrden().get("motivo_rechazo")).isNull();
        assertThat(filaOrden().get("version")).isEqualTo(0);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM eventos_salida", Integer.class)).isZero();
    }

    // ----- Débito tardío -----

    @Test
    void pagoEnEfectivoMientrasSeCobrabaYLuegoDebitoTardioSeDevuelve() {
        esStatus(pagarConCartera(), 202);

        // el cliente paga en mostrador antes de que Wallet responda
        ResponseEntity<String> efectivo = pagarEnEfectivo();
        esStatus(efectivo, 200);
        assertThat(PruebaIntegracion.<String>json(efectivo, "$.estado")).isEqualTo("PAGADA");
        assertThat(PruebaIntegracion.<String>json(efectivo, "$.metodoPago")).isEqualTo("EFECTIVO");

        UUID tardio = carteraDebitada();

        await().atMost(ESPERA).until(() -> procesado(tardio));
        assertThat(estadoOrden(orden)).isEqualTo("PAGADA");
        assertThat(filaOrden().get("metodo_pago")).isEqualTo("EFECTIVO");
        assertThat(enOutbox("AlquilerPagado")).hasSize(1);                         // solo el de S13

        String reembolso = esperarEventos("ReembolsoCarteraSolicitado").get("ReembolsoCarteraSolicitado");
        assertThat(JsonPath.<String>read(reembolso, "$.aggregateId")).isEqualTo(orden.toString());
        assertThat(JsonPath.<String>read(reembolso, "$.payload.referenciaTipo")).isEqualTo("ALQUILER");
        assertThat(JsonPath.<String>read(reembolso, "$.payload.referenciaId")).isEqualTo(orden.toString());
        assertThat(JsonPath.<String>read(reembolso, "$.payload.usuarioId")).isEqualTo(cliente.toString());
        assertThat(decimal(reembolso, "$.payload.monto")).isEqualByComparingTo("2600.00");
        assertThat(JsonPath.<String>read(reembolso, "$.payload.motivo")).isEqualTo("DEBITO_TARDIO");
        assertThat(JsonPath.<String>read(reembolso, "$.payload.fechaMovimiento"))
                .isEqualTo(fechaNoAnteriorAlPago().toString());

        // Wallet devuelve el dinero: la orden sigue PAGADA en efectivo
        UUID reembolsada = carteraReembolsada();
        await().atMost(ESPERA).until(() -> procesado(reembolsada));
        assertThat(estadoOrden(orden)).isEqualTo("PAGADA");
        assertThat(filaOrden().get("metodo_pago")).isEqualTo("EFECTIVO");
    }

    @Test
    void carteraReembolsadaDeUnAlquilerNoCambiaLaOrden() {
        esStatus(pagarConCartera(), 202);
        responderRechazo("SALDO_INSUFICIENTE");
        await().atMost(ESPERA).untilAsserted(() -> assertThat(estadoOrden(orden)).isEqualTo("RECHAZADA"));
        Object version = filaOrden().get("version");

        UUID reembolsada = carteraReembolsada();

        await().atMost(ESPERA).until(() -> procesado(reembolsada));
        assertThat(estadoOrden(orden)).isEqualTo("RECHAZADA");                  // no pasa a REEMBOLSADA
        assertThat(filaOrden().get("version")).isEqualTo(version);
        // y se puede seguir pagando
        esStatus(pagarConCartera(), 202);
    }
}
