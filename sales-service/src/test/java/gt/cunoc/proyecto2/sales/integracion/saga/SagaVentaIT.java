package gt.cunoc.proyecto2.sales.integracion.saga;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.jayway.jsonpath.JsonPath;
import gt.cunoc.proyecto2.sales.integracion.soporte.PruebaIntegracion;
import gt.cunoc.proyecto2.sales.servicios.ventas.VentaServicio;
import gt.cunoc.proyecto2.sales.utilidades.Fechas;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;

/**
 * Saga de la venta en línea de punta a punta: S2 publica DebitoCarteraSolicitado, la prueba responde como
 * Identity (CarteraDebitada, DebitoCarteraRechazado, CarteraReembolsada en identity.eventos) y se revisan
 * la venta, los boletos, los asientos y los eventos que publica Sales.
 */
class SagaVentaIT extends PruebaIntegracion {

    private static final Duration ESPERA = Duration.ofSeconds(15);
    private static final String EN_LINEA = "/sales/ventas/en-linea";

    @Autowired
    private VentaServicio ventaServicio;

    private UUID xela;
    private UUID cliente;
    private UUID viaje;

    @BeforeEach
    void datos() {
        xela = UUID.randomUUID();
        cliente = UUID.randomUUID();
        insertarCliente(cliente, "1234567-8", "Ana Pérez", "ana@correo.com");
        viaje = insertarViaje(xela);                       // 40 asientos, Q75
        tripsVendible(viaje);
    }

    // ----- Ayudas -----

    private ResponseEntity<String> comprar(Integer... asientos) {
        return post(EN_LINEA, tokenCliente(cliente), """
                {"viajeId":"%s","asientos":%s,"fechaPago":"%s"}""".formatted(viaje, List.of(asientos), FECHA_PAGO));
    }

    /** S2 aceptada: devuelve el id de la venta PENDIENTE_PAGO. */
    private UUID comprarEnLinea(Integer... asientos) {
        ResponseEntity<String> respuesta = comprar(asientos);
        esStatus(respuesta, 202);
        return UUID.fromString(json(respuesta, "$.ventaId"));
    }

    private UUID carteraDebitada(UUID ventaId, String monto) {
        return publicarEvento(IDENTITY_EVENTOS, "CarteraDebitada", cliente, null,
                movimientoCartera("VENTA", ventaId, cliente, monto));
    }

    private Map<String, Object> venta(UUID ventaId) {
        return jdbc.queryForMap("SELECT * FROM ventas WHERE id = ?", ventaId);
    }

    private int boletos(UUID ventaId) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM boletos WHERE venta_id = ?", Integer.class, ventaId);
        return total == null ? 0 : total;
    }

    /** Asientos que siguen ligados a la venta (venta_id). */
    private int asientosDeVenta(UUID ventaId) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM asientos_viaje WHERE venta_id = ?",
                Integer.class, ventaId);
        return total == null ? 0 : total;
    }

    /** Sobres JSON guardados en el outbox de un tipo, en orden de creación. */
    private List<String> enOutbox(String tipo) {
        return jdbc.queryForList("SELECT datos::text FROM eventos_salida WHERE tipo_evento = ? ORDER BY fecha_creacion",
                String.class, tipo);
    }

    /** Fecha del reembolso por débito tardío: hoy en Guatemala, o la fecha de pago si es posterior. */
    private static LocalDate fechaNoAnteriorAlPago() {
        LocalDate hoy = Fechas.hoy();
        return hoy.isBefore(FECHA_PAGO) ? FECHA_PAGO : hoy;
    }

    private void verificarReembolsoDebitoTardio(UUID ventaId, String monto) {
        String reembolso = esperarEventos("ReembolsoCarteraSolicitado").get("ReembolsoCarteraSolicitado");
        assertThat(JsonPath.<String>read(reembolso, "$.aggregateId")).isEqualTo(ventaId.toString());
        assertThat(JsonPath.<String>read(reembolso, "$.payload.referenciaTipo")).isEqualTo("VENTA");
        assertThat(JsonPath.<String>read(reembolso, "$.payload.referenciaId")).isEqualTo(ventaId.toString());
        assertThat(JsonPath.<String>read(reembolso, "$.payload.usuarioId")).isEqualTo(cliente.toString());
        assertThat(decimal(reembolso, "$.payload.monto")).isEqualByComparingTo(monto);
        assertThat(JsonPath.<String>read(reembolso, "$.payload.motivo")).isEqualTo("DEBITO_TARDIO");
        assertThat(JsonPath.<String>read(reembolso, "$.payload.fechaMovimiento"))
                .isEqualTo(fechaNoAnteriorAlPago().toString());
        assertThat(enOutbox("ReembolsoCarteraSolicitado")).hasSize(1);
    }

    // ----- Camino feliz -----

    @Test
    void carteraDebitadaConfirmaLaVentaEmiteBoletosYPublicaBoletoVendido() {
        ResponseEntity<String> respuesta = comprar(3, 4);

        esStatus(respuesta, 202);
        UUID ventaId = UUID.fromString(json(respuesta, "$.ventaId"));
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.estado")).isEqualTo("PENDIENTE_PAGO");
        assertThat(decimal(respuesta, "$.montoTotal")).isEqualByComparingTo("150.00");
        assertThat(PruebaIntegracion.<List<Integer>>json(respuesta, "$.asientos")).containsExactly(3, 4);
        assertThat(estadosAsientos(viaje, 3, 4)).containsExactly("RESERVADO", "RESERVADO");

        // Identity recibe la solicitud de débito
        String debito = esperarEventos("DebitoCarteraSolicitado").get("DebitoCarteraSolicitado");
        assertThat(JsonPath.<String>read(debito, "$.aggregateId")).isEqualTo(ventaId.toString());
        assertThat(JsonPath.<String>read(debito, "$.businessDate")).isEqualTo(FECHA_PAGO.toString());
        assertThat(JsonPath.<String>read(debito, "$.payload.referenciaTipo")).isEqualTo("VENTA");
        assertThat(JsonPath.<String>read(debito, "$.payload.referenciaId")).isEqualTo(ventaId.toString());
        assertThat(JsonPath.<String>read(debito, "$.payload.usuarioId")).isEqualTo(cliente.toString());
        assertThat(decimal(debito, "$.payload.monto")).isEqualByComparingTo("150.00");
        assertThat(JsonPath.<String>read(debito, "$.payload.fechaMovimiento")).isEqualTo(FECHA_PAGO.toString());

        // ... y responde que cobró
        carteraDebitada(ventaId, "150.00");

        await().atMost(ESPERA).untilAsserted(() -> assertThat(estadoVenta(ventaId)).isEqualTo("CONFIRMADA"));
        assertThat(estadosAsientos(viaje, 3, 4)).containsExactly("VENDIDO", "VENDIDO");
        assertThat(jdbc.queryForObject(
                "SELECT count(*) FROM asientos_viaje WHERE venta_id = ? AND reservado_hasta IS NULL",
                Integer.class, ventaId)).isEqualTo(2);
        List<Map<String, Object>> emitidos = jdbc.queryForList(
                "SELECT numero_asiento, precio, estado, viaje_id FROM boletos WHERE venta_id = ? "
                        + "ORDER BY numero_asiento",
                ventaId);
        assertThat(emitidos.stream().map(fila -> fila.get("numero_asiento")).toList()).containsExactly(3, 4);
        assertThat(emitidos.stream().map(fila -> fila.get("estado")).toList()).containsOnly("ACTIVO");
        assertThat(emitidos.stream().map(fila -> fila.get("viaje_id")).toList()).containsOnly(viaje);
        assertThat((BigDecimal) emitidos.get(0).get("precio")).isEqualByComparingTo("75.00");

        String vendido = esperarEventos("BoletoVendido").get("BoletoVendido");
        assertThat(JsonPath.<String>read(vendido, "$.aggregateId")).isEqualTo(ventaId.toString());
        assertThat(JsonPath.<String>read(vendido, "$.businessDate")).isEqualTo(FECHA_PAGO.toString());
        assertThat(JsonPath.<String>read(vendido, "$.payload.ventaId")).isEqualTo(ventaId.toString());
        assertThat(JsonPath.<String>read(vendido, "$.payload.viajeId")).isEqualTo(viaje.toString());
        assertThat(JsonPath.<String>read(vendido, "$.payload.sucursalId")).isEqualTo(xela.toString());
        assertThat(JsonPath.<String>read(vendido, "$.payload.canal")).isEqualTo("EN_LINEA");
        assertThat(JsonPath.<String>read(vendido, "$.payload.metodoPago")).isEqualTo("CARTERA");
        assertThat(JsonPath.<String>read(vendido, "$.payload.clienteNit")).isEqualTo("1234567-8");
        assertThat(JsonPath.<String>read(vendido, "$.payload.clienteCorreo")).isEqualTo("ana@correo.com");
        assertThat(JsonPath.<List<Integer>>read(vendido, "$.payload.asientos")).containsExactly(3, 4);
        assertThat(JsonPath.<Integer>read(vendido, "$.payload.cantidadBoletos")).isEqualTo(2);
        assertThat(decimal(vendido, "$.payload.montoTotal")).isEqualByComparingTo("150.00");
        assertThat(JsonPath.<String>read(vendido, "$.payload.busPlaca")).isEqualTo("P-123ABC");
        assertThat(JsonPath.<String>read(vendido, "$.payload.origenNombre")).isEqualTo("Quetzaltenango");

        // S3: el frontend ve la venta confirmada con sus boletos
        ResponseEntity<String> detalle = get("/sales/ventas/" + ventaId, tokenCliente(cliente));
        esStatus(detalle, 200);
        assertThat(PruebaIntegracion.<String>json(detalle, "$.estado")).isEqualTo("CONFIRMADA");
        assertThat(PruebaIntegracion.<Integer>json(detalle, "$.boletos.length()")).isEqualTo(2);
    }

    // ----- Rechazo de Identity -----

    @Test
    void debitoRechazadoRechazaLaVentaYLiberaLosAsientos() {
        UUID ventaId = comprarEnLinea(5, 6);

        publicarEvento(IDENTITY_EVENTOS, "DebitoCarteraRechazado", cliente, null,
                debitoRechazado("VENTA", ventaId, cliente, "150.00", "SALDO_INSUFICIENTE"));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(estadoVenta(ventaId)).isEqualTo("RECHAZADA"));
        assertThat(venta(ventaId).get("motivo_rechazo")).isEqualTo("SALDO_INSUFICIENTE");
        assertThat(estadosAsientos(viaje, 5, 6)).containsExactly("DISPONIBLE", "DISPONIBLE");
        assertThat(asientosDeVenta(ventaId)).isZero();
        assertThat(boletos(ventaId)).isZero();
        assertThat(enOutbox("BoletoVendido")).isEmpty();
        assertThat(enOutbox("ReembolsoCarteraSolicitado")).isEmpty();

        // los asientos se pueden volver a comprar
        UUID otra = comprarEnLinea(5, 6);
        assertThat(estadosAsientos(viaje, 5, 6)).containsExactly("RESERVADO", "RESERVADO");
        assertThat(otra).isNotEqualTo(ventaId);
    }

    @Test
    void unRechazoSinMotivoQuedaComoRechazado() {
        UUID ventaId = comprarEnLinea(7);

        publicarEvento(IDENTITY_EVENTOS, "DebitoCarteraRechazado", cliente, null, """
                {"referenciaTipo":"VENTA","referenciaId":"%s","usuarioId":"%s","monto":75.00,"motivo":null}
                """.formatted(ventaId, cliente));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(estadoVenta(ventaId)).isEqualTo("RECHAZADA"));
        assertThat(venta(ventaId).get("motivo_rechazo")).isEqualTo("RECHAZADO");
    }

    // ----- Débito tardío -----

    @Test
    void debitoTardioDeUnaVentaQuePerdioSusAsientosSeRechazaYSeDevuelve() {
        UUID ventaId = comprarEnLinea(7);
        esperarEventos("DebitoCarteraSolicitado");
        // la reserva venció y otra tarea liberó los asientos antes de que Identity cobrara
        jdbc.update("""
                UPDATE asientos_viaje SET estado = 'DISPONIBLE', venta_id = NULL, reservado_hasta = NULL
                 WHERE venta_id = ?""", ventaId);

        carteraDebitada(ventaId, "75.00");

        await().atMost(ESPERA).untilAsserted(() -> assertThat(estadoVenta(ventaId)).isEqualTo("RECHAZADA"));
        assertThat(venta(ventaId).get("motivo_rechazo")).isEqualTo("TIEMPO_AGOTADO");
        assertThat(boletos(ventaId)).isZero();
        assertThat(estadosAsientos(viaje, 7)).containsExactly("DISPONIBLE");
        assertThat(enOutbox("BoletoVendido")).isEmpty();
        verificarReembolsoDebitoTardio(ventaId, "75.00");

        // Identity devuelve el dinero
        publicarEvento(IDENTITY_EVENTOS, "CarteraReembolsada", cliente, null,
                movimientoCartera("VENTA", ventaId, cliente, "75.00"));

        await().atMost(ESPERA).untilAsserted(() -> assertThat(estadoVenta(ventaId)).isEqualTo("REEMBOLSADA"));
    }

    @Test
    void debitoTardioDeUnaVentaYaRechazadaPideElReembolso() {
        UUID ventaId = comprarEnLinea(8, 9);
        jdbc.update("UPDATE ventas SET estado = 'RECHAZADA', motivo_rechazo = 'TIEMPO_AGOTADO' WHERE id = ?", ventaId);
        jdbc.update("""
                UPDATE asientos_viaje SET estado = 'DISPONIBLE', venta_id = NULL, reservado_hasta = NULL
                 WHERE venta_id = ?""", ventaId);

        UUID eventId = carteraDebitada(ventaId, "150.00");

        await().atMost(ESPERA).until(() -> procesado(eventId));
        assertThat(estadoVenta(ventaId)).isEqualTo("RECHAZADA");
        assertThat(boletos(ventaId)).isZero();
        verificarReembolsoDebitoTardio(ventaId, "150.00");

        publicarEvento(IDENTITY_EVENTOS, "CarteraReembolsada", cliente, null,
                movimientoCartera("VENTA", ventaId, cliente, "150.00"));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(estadoVenta(ventaId)).isEqualTo("REEMBOLSADA"));
    }

    @Test
    void debitoDeUnViajeQueYaSalioRechazaLaVentaYDevuelve() {
        UUID ventaId = comprarEnLinea(10);
        jdbc.update("UPDATE vista_viajes SET estado = 'EN_RUTA' WHERE viaje_id = ?", viaje);

        carteraDebitada(ventaId, "75.00");

        await().atMost(ESPERA).untilAsserted(() -> assertThat(estadoVenta(ventaId)).isEqualTo("RECHAZADA"));
        assertThat(venta(ventaId).get("motivo_rechazo")).isEqualTo("VIAJE_NO_DISPONIBLE");
        assertThat(estadosAsientos(viaje, 10)).containsExactly("DISPONIBLE");
        assertThat(boletos(ventaId)).isZero();
        verificarReembolsoDebitoTardio(ventaId, "75.00");
    }

    // ----- Respuestas repetidas -----

    @Test
    void respuestasRepetidasSobreUnaVentaConfirmadaNoCambianNada() {
        UUID ventaId = comprarEnLinea(11, 12);
        carteraDebitada(ventaId, "150.00");
        await().atMost(ESPERA).untilAsserted(() -> assertThat(estadoVenta(ventaId)).isEqualTo("CONFIRMADA"));
        Integer version = (Integer) venta(ventaId).get("version");

        // Identity repite el débito (otro eventId), y llegan un rechazo y un reembolso que no corresponden
        UUID repetido = carteraDebitada(ventaId, "150.00");
        UUID rechazo = publicarEvento(IDENTITY_EVENTOS, "DebitoCarteraRechazado", cliente, null,
                debitoRechazado("VENTA", ventaId, cliente, "150.00", "SALDO_INSUFICIENTE"));
        UUID reembolso = publicarEvento(IDENTITY_EVENTOS, "CarteraReembolsada", cliente, null,
                movimientoCartera("VENTA", ventaId, cliente, "150.00"));

        await().atMost(ESPERA).until(() -> procesado(repetido) && procesado(rechazo) && procesado(reembolso));
        assertThat(estadoVenta(ventaId)).isEqualTo("CONFIRMADA");
        assertThat(venta(ventaId).get("version")).isEqualTo(version);
        assertThat(boletos(ventaId)).isEqualTo(2);
        assertThat(estadosAsientos(viaje, 11, 12)).containsExactly("VENDIDO", "VENDIDO");
        assertThat(enOutbox("BoletoVendido")).hasSize(1);
        assertThat(enOutbox("ReembolsoCarteraSolicitado")).isEmpty();
    }

    @Test
    void respuestasDeIdentityDeVentasInexistentesSeIgnoran() {
        UUID inexistente = UUID.randomUUID();

        UUID debito = carteraDebitada(inexistente, "75.00");
        UUID rechazo = publicarEvento(IDENTITY_EVENTOS, "DebitoCarteraRechazado", cliente, null,
                debitoRechazado("VENTA", inexistente, cliente, "75.00", "USUARIO_INACTIVO"));
        UUID reembolso = publicarEvento(IDENTITY_EVENTOS, "CarteraReembolsada", cliente, null,
                movimientoCartera("VENTA", inexistente, cliente, "75.00"));

        await().atMost(ESPERA).until(() -> procesado(debito) && procesado(rechazo) && procesado(reembolso));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM ventas", Integer.class)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM eventos_salida", Integer.class)).isZero();
    }

    // ----- Tarea de reservas vencidas -----

    @Test
    void laTareaRechazaPorTiempoLasReservasVencidasYLiberaLosAsientos() {
        UUID vencida = comprarEnLinea(13, 14);
        UUID vigente = comprarEnLinea(15);
        // la venta se creó hace 10 minutos y su reserva ya venció
        jdbc.update("UPDATE ventas SET fecha_creacion = now() - interval '10 minutes' WHERE id = ?", vencida);
        jdbc.update("UPDATE asientos_viaje SET reservado_hasta = now() - interval '1 minute' WHERE venta_id = ?",
                vencida);

        int rechazadas = ventaServicio.liberarReservasVencidas();

        assertThat(rechazadas).isEqualTo(1);
        assertThat(estadoVenta(vencida)).isEqualTo("RECHAZADA");
        assertThat(venta(vencida).get("motivo_rechazo")).isEqualTo("TIEMPO_AGOTADO");
        assertThat(estadosAsientos(viaje, 13, 14)).containsExactly("DISPONIBLE", "DISPONIBLE");
        assertThat(asientosDeVenta(vencida)).isZero();
        // la otra sigue esperando a Identity
        assertThat(estadoVenta(vigente)).isEqualTo("PENDIENTE_PAGO");
        assertThat(estadosAsientos(viaje, 15)).containsExactly("RESERVADO");

        // si Identity cobra después, se devuelve el dinero
        carteraDebitada(vencida, "150.00");
        await().atMost(ESPERA).untilAsserted(() -> assertThat(enOutbox("ReembolsoCarteraSolicitado")).hasSize(1));
        assertThat(estadoVenta(vencida)).isEqualTo("RECHAZADA");
        assertThat(boletos(vencida)).isZero();
    }
}
