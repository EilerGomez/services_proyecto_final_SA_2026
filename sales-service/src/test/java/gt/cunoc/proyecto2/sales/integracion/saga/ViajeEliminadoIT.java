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
 * Compensación por ViajeEliminado (Trips): las ventas pendientes se rechazan, las confirmadas se cancelan
 * (con reembolso si se pagaron con cartera), los boletos se anulan y el viaje sale de la vista con sus asientos.
 */
class ViajeEliminadoIT extends PruebaIntegracion {

    private static final Duration ESPERA = Duration.ofSeconds(15);

    private UUID xela;
    private UUID cliente;
    private UUID viaje;

    @BeforeEach
    void datos() {
        xela = UUID.randomUUID();
        cliente = UUID.randomUUID();
        insertarCliente(cliente, "1234567-8", "Ana Pérez", "ana@correo.com");
        viaje = insertarViaje(xela);                       // 40 asientos, Q75, version 0
        tripsVendible(viaje);
    }

    // ----- Ayudas -----

    /** S4: venta en mostrador CONFIRMADA (efectivo). */
    private UUID venderEnMostrador(Integer... asientos) {
        ResponseEntity<String> respuesta = post("/sales/ventas/mostrador", tokenCajero(xela), """
                {"viajeId":"%s","asientos":%s,"clienteNit":null,"clienteNombre":"Juan López","clienteCorreo":null,
                 "fechaPago":"%s"}""".formatted(viaje, List.of(asientos), FECHA_PAGO));
        esStatus(respuesta, 201);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.estado")).isEqualTo("CONFIRMADA");
        return UUID.fromString(json(respuesta, "$.ventaId"));
    }

    /** S2: venta en línea PENDIENTE_PAGO. */
    private UUID comprarEnLinea(Integer... asientos) {
        ResponseEntity<String> respuesta = post("/sales/ventas/en-linea", tokenCliente(cliente), """
                {"viajeId":"%s","asientos":%s,"fechaPago":"%s"}""".formatted(viaje, List.of(asientos), FECHA_PAGO));
        esStatus(respuesta, 202);
        return UUID.fromString(json(respuesta, "$.ventaId"));
    }

    /** S2 + CarteraDebitada: venta en línea CONFIRMADA por la saga. */
    private UUID comprarYConfirmarEnLinea(Integer... asientos) {
        UUID ventaId = comprarEnLinea(asientos);
        carteraDebitada(ventaId, "75.00");
        await().atMost(ESPERA).untilAsserted(() -> assertThat(estadoVenta(ventaId)).isEqualTo("CONFIRMADA"));
        return ventaId;
    }

    private UUID carteraDebitada(UUID ventaId, String monto) {
        return publicarEvento(IDENTITY_EVENTOS, "CarteraDebitada", cliente, null,
                movimientoCartera("VENTA", ventaId, cliente, monto));
    }

    private UUID viajeEliminado(String tipo, String motivo) {
        return publicarEvento(TRIPS_EVENTOS, "ViajeEliminado", viaje, 1L, """
                {"viajeId":"%s","tipo":"%s","sucursalId":"%s","motivo":%s}
                """.formatted(viaje, tipo, xela, motivo == null ? "null" : "\"" + motivo + "\""));
    }

    private Map<String, Object> venta(UUID ventaId) {
        return jdbc.queryForMap("SELECT * FROM ventas WHERE id = ?", ventaId);
    }

    private int contarDonde(String tabla, String condicion, Object... valores) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM " + tabla + " WHERE " + condicion, Integer.class,
                valores);
        return total == null ? 0 : total;
    }

    private List<String> enOutbox(String tipo) {
        return jdbc.queryForList("SELECT datos::text FROM eventos_salida WHERE tipo_evento = ? ORDER BY fecha_creacion",
                String.class, tipo);
    }

    /** Espera a que el relay publique todo el outbox y devuelve lo que llegó a la cola espía. */
    private List<String> publicadosEnRabbit() {
        await().atMost(ESPERA).until(() -> contarDonde("eventos_salida", "fecha_publicacion IS NULL") == 0);
        return recibirDurante(1500);
    }

    private static List<String> deTipo(List<String> sobres, String tipo) {
        return sobres.stream()
                .filter(sobre -> tipo.equals(JsonPath.read(sobre, "$.eventType")))
                .toList();
    }

    private static String deVenta(List<String> sobres, UUID ventaId) {
        return sobres.stream()
                .filter(sobre -> ventaId.toString().equals(JsonPath.read(sobre, "$.payload.ventaId")))
                .findFirst()
                .orElseThrow();
    }

    /** Cancelación automática: hoy en Guatemala, o la fecha de pago si es posterior. */
    private static LocalDate fechaNoAnteriorAlPago() {
        LocalDate hoy = Fechas.hoy();
        return hoy.isBefore(FECHA_PAGO) ? FECHA_PAGO : hoy;
    }

    // ----- Compensación -----

    @Test
    void viajeEliminadoCancelaRechazaYReembolsaLasVentasDelViaje() {
        UUID mostrador = venderEnMostrador(1, 2);
        UUID lineaConfirmada = comprarYConfirmarEnLinea(3);
        UUID lineaPendiente = comprarEnLinea(4);
        assertThat(contarDonde("boletos", "viaje_id = ? AND estado = 'ACTIVO'", viaje)).isEqualTo(3);

        viajeEliminado("REGULAR", "Bus averiado");

        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(contarDonde("vista_viajes", "viaje_id = ?", viaje)).isZero());
        String fechaCancelacion = fechaNoAnteriorAlPago().toString();

        // mostrador: CANCELADA con el motivo de Trips
        Map<String, Object> deMostrador = venta(mostrador);
        assertThat(deMostrador.get("estado")).isEqualTo("CANCELADA");
        assertThat(deMostrador.get("motivo_cancelacion")).isEqualTo("Bus averiado");
        assertThat(deMostrador.get("fecha_cancelacion").toString()).isEqualTo(fechaCancelacion);
        // en línea confirmada: CANCELADA (y se pide el reembolso)
        Map<String, Object> enLinea = venta(lineaConfirmada);
        assertThat(enLinea.get("estado")).isEqualTo("CANCELADA");
        assertThat(enLinea.get("motivo_cancelacion")).isEqualTo("Bus averiado");
        // en línea pendiente: RECHAZADA
        assertThat(estadoVenta(lineaPendiente)).isEqualTo("RECHAZADA");
        assertThat(venta(lineaPendiente).get("motivo_rechazo")).isEqualTo("VIAJE_ELIMINADO");

        // boletos anulados; el viaje y sus asientos ya no están (ventas y boletos se conservan)
        assertThat(contarDonde("boletos", "viaje_id = ? AND estado = 'ACTIVO'", viaje)).isZero();
        assertThat(contarDonde("boletos", "viaje_id = ? AND estado = 'CANCELADO'", viaje)).isEqualTo(3);
        assertThat(contarDonde("asientos_viaje", "viaje_id = ?", viaje)).isZero();
        assertThat(contarDonde("ventas", "viaje_id = ?", viaje)).isEqualTo(3);

        // eventos: 2 VentaCancelada y 1 reembolso (solo la pagada con cartera)
        assertThat(enOutbox("VentaCancelada")).hasSize(2);
        assertThat(enOutbox("ReembolsoCarteraSolicitado")).hasSize(1);
        List<String> sobres = publicadosEnRabbit();
        List<String> canceladas = deTipo(sobres, "VentaCancelada");
        assertThat(canceladas).hasSize(2);

        String canceladaMostrador = deVenta(canceladas, mostrador);
        assertThat(JsonPath.<String>read(canceladaMostrador, "$.businessDate")).isEqualTo(fechaCancelacion);
        assertThat(JsonPath.<String>read(canceladaMostrador, "$.payload.viajeId")).isEqualTo(viaje.toString());
        assertThat(JsonPath.<String>read(canceladaMostrador, "$.payload.metodoPago")).isEqualTo("EFECTIVO");
        assertThat(JsonPath.<String>read(canceladaMostrador, "$.payload.motivo")).isEqualTo("Bus averiado");
        assertThat(JsonPath.<List<Integer>>read(canceladaMostrador, "$.payload.asientos")).containsExactly(1, 2);
        assertThat(JsonPath.<Integer>read(canceladaMostrador, "$.payload.cantidadBoletos")).isEqualTo(2);
        assertThat(decimal(canceladaMostrador, "$.payload.montoAnulado")).isEqualByComparingTo("150.00");
        assertThat(JsonPath.<String>read(canceladaMostrador, "$.payload.fechaCancelacion")).isEqualTo(fechaCancelacion);
        // se arma antes de borrar el viaje: lleva origen y destino
        assertThat(JsonPath.<String>read(canceladaMostrador, "$.payload.origenNombre")).isEqualTo("Quetzaltenango");
        assertThat(JsonPath.<String>read(canceladaMostrador, "$.payload.destinoNombre")).isEqualTo("Guatemala");

        String canceladaEnLinea = deVenta(canceladas, lineaConfirmada);
        assertThat(JsonPath.<String>read(canceladaEnLinea, "$.payload.metodoPago")).isEqualTo("CARTERA");
        assertThat(JsonPath.<String>read(canceladaEnLinea, "$.payload.clienteUsuarioId")).isEqualTo(cliente.toString());
        assertThat(JsonPath.<List<Integer>>read(canceladaEnLinea, "$.payload.asientos")).containsExactly(3);

        List<String> reembolsos = deTipo(sobres, "ReembolsoCarteraSolicitado");
        assertThat(reembolsos).hasSize(1);
        String reembolso = reembolsos.get(0);
        assertThat(JsonPath.<String>read(reembolso, "$.payload.referenciaTipo")).isEqualTo("VENTA");
        assertThat(JsonPath.<String>read(reembolso, "$.payload.referenciaId")).isEqualTo(lineaConfirmada.toString());
        assertThat(JsonPath.<String>read(reembolso, "$.payload.usuarioId")).isEqualTo(cliente.toString());
        assertThat(JsonPath.<String>read(reembolso, "$.payload.motivo")).isEqualTo("VIAJE_ELIMINADO");
        assertThat(decimal(reembolso, "$.payload.monto")).isEqualByComparingTo("75.00");
        assertThat(JsonPath.<String>read(reembolso, "$.payload.fechaMovimiento")).isEqualTo(fechaCancelacion);

        // Identity devuelve el dinero de la venta en línea
        publicarEvento(IDENTITY_EVENTOS, "CarteraReembolsada", cliente, null,
                movimientoCartera("VENTA", lineaConfirmada, cliente, "75.00"));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(estadoVenta(lineaConfirmada)).isEqualTo("REEMBOLSADA"));
        assertThat(estadoVenta(mostrador)).isEqualTo("CANCELADA");
    }

    @Test
    void sinMotivoSeUsaViajeEliminadoYUnDebitoQueLlegaDespuesSeDevuelve() {
        UUID mostrador = venderEnMostrador(5);
        UUID pendiente = comprarEnLinea(6);

        viajeEliminado("REGULAR", null);

        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(contarDonde("vista_viajes", "viaje_id = ?", viaje)).isZero());
        assertThat(venta(mostrador).get("motivo_cancelacion")).isEqualTo("VIAJE_ELIMINADO");
        assertThat(estadoVenta(pendiente)).isEqualTo("RECHAZADA");
        assertThat(enOutbox("VentaCancelada")).hasSize(1);                   // solo la de mostrador
        assertThat(enOutbox("ReembolsoCarteraSolicitado")).isEmpty();        // efectivo y aún sin cobro

        // Identity cobra la pendiente cuando el viaje ya no existe: débito tardío
        carteraDebitada(pendiente, "75.00");

        await().atMost(ESPERA).untilAsserted(() -> assertThat(enOutbox("ReembolsoCarteraSolicitado")).hasSize(1));
        String reembolso = enOutbox("ReembolsoCarteraSolicitado").get(0);
        assertThat(JsonPath.<String>read(reembolso, "$.payload.referenciaId")).isEqualTo(pendiente.toString());
        assertThat(JsonPath.<String>read(reembolso, "$.payload.motivo")).isEqualTo("DEBITO_TARDIO");
        assertThat(estadoVenta(pendiente)).isEqualTo("RECHAZADA");
        assertThat(contarDonde("boletos", "venta_id = ?", pendiente)).isZero();

        publicarEvento(IDENTITY_EVENTOS, "CarteraReembolsada", cliente, null,
                movimientoCartera("VENTA", pendiente, cliente, "75.00"));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(estadoVenta(pendiente)).isEqualTo("REEMBOLSADA"));
    }

    @Test
    void viajeEliminadoDeUnViajeSinVentasSoloLoBorra() {
        viajeEliminado("REGULAR", "Ruta cerrada");

        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(contarDonde("vista_viajes", "viaje_id = ?", viaje)).isZero());
        assertThat(contarDonde("asientos_viaje", "viaje_id = ?", viaje)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM eventos_salida", Integer.class)).isZero();
    }

    @Test
    void viajeEliminadoPrivadoNoHaceNada() {
        UUID mostrador = venderEnMostrador(7);

        UUID eventId = viajeEliminado("PRIVADO", "Alquiler cancelado");

        await().atMost(ESPERA).until(() -> procesado(eventId));
        assertThat(contarDonde("vista_viajes", "viaje_id = ?", viaje)).isEqualTo(1);
        assertThat(contarDonde("asientos_viaje", "viaje_id = ?", viaje)).isEqualTo(40);
        assertThat(estadoVenta(mostrador)).isEqualTo("CONFIRMADA");
        assertThat(contarDonde("boletos", "venta_id = ? AND estado = 'ACTIVO'", mostrador)).isEqualTo(1);
        assertThat(estadosAsientos(viaje, 7)).containsExactly("VENDIDO");
        assertThat(enOutbox("VentaCancelada")).isEmpty();
    }
}
