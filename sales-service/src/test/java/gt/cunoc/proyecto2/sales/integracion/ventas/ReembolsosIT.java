package gt.cunoc.proyecto2.sales.integracion.ventas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.jayway.jsonpath.JsonPath;
import gt.cunoc.proyecto2.sales.integracion.soporte.PruebaIntegracion;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * S14 · Devolución del dinero de una venta cancelada en la sucursal.
 *
 * El caso de todos los días: se elimina un viaje, las compras de mostrador quedan canceladas y el
 * cliente llega con su boleto impreso. La sucursal lo escanea (busca por el código de barras),
 * revisa la venta y le entrega el efectivo.
 */
class ReembolsosIT extends PruebaIntegracion {

    private static final Duration ESPERA = Duration.ofSeconds(15);
    private static final String VENTAS = "/sales/ventas";

    private UUID xela;
    private UUID guate;
    private UUID cliente;
    private UUID viaje;

    @BeforeEach
    void datos() {
        xela = UUID.randomUUID();
        guate = UUID.randomUUID();
        cliente = UUID.randomUUID();
        insertarCliente(cliente, "1234567-8", "Ana Pérez", "ana@correo.com");
        viaje = insertarViaje(xela);
        tripsVendible(viaje);
    }

    // ----- Ayudas -----

    private UUID venderEnMostrador(Integer... asientos) {
        ResponseEntity<String> respuesta = post(VENTAS + "/mostrador", tokenCajero(xela), """
                {"viajeId":"%s","asientos":%s,"clienteNit":"1234567-8","clienteNombre":"Ana Pérez",
                 "clienteCorreo":"ana@correo.com","fechaPago":"%s"}"""
                .formatted(viaje, List.of(asientos), FECHA_PAGO));
        esStatus(respuesta, 201);
        return UUID.fromString(json(respuesta, "$.ventaId"));
    }

    private UUID comprarYConfirmarEnLinea(Integer... asientos) {
        ResponseEntity<String> respuesta = post(VENTAS + "/en-linea", tokenCliente(cliente), """
                {"viajeId":"%s","asientos":%s,"fechaPago":"%s"}"""
                .formatted(viaje, List.of(asientos), FECHA_PAGO));
        esStatus(respuesta, 202);
        UUID ventaId = UUID.fromString(json(respuesta, "$.ventaId"));
        publicarEvento(WALLET_EVENTOS, "CarteraDebitada", cliente, null,
                movimientoCartera("VENTA", ventaId, cliente, "75.00"));
        await().atMost(ESPERA).untilAsserted(() -> assertThat(estadoVenta(ventaId)).isEqualTo("CONFIRMADA"));
        return ventaId;
    }

    /** Elimina el viaje en Trips y espera a que Sales cancele las ventas y borre la proyección. */
    private void eliminarViaje(String motivo) {
        publicarEvento(TRIPS_EVENTOS, "ViajeEliminado", viaje, 1L, """
                {"viajeId":"%s","tipo":"REGULAR","sucursalId":"%s","motivo":"%s"}"""
                .formatted(viaje, xela, motivo));
        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(contar("vista_viajes", "viaje_id = ?", viaje)).isZero());
    }

    private Map<String, Object> venta(UUID ventaId) {
        return jdbc.queryForMap("SELECT * FROM ventas WHERE id = ?", ventaId);
    }

    private int contar(String tabla, String condicion, Object... valores) {
        Integer total = jdbc.queryForObject("SELECT count(*) FROM " + tabla + " WHERE " + condicion,
                Integer.class, valores);
        return total == null ? 0 : total;
    }

    private List<String> enOutbox(String tipo) {
        return jdbc.queryForList(
                "SELECT datos::text FROM eventos_salida WHERE tipo_evento = ? ORDER BY fecha_creacion",
                String.class, tipo);
    }

    private static String reembolso(int version) {
        return "{\"version\":%d}".formatted(version);
    }

    /** Código del boleto impreso: los primeros 12 caracteres del id, en mayúsculas. */
    private static String codigo(UUID ventaId) {
        return ventaId.toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    // ----- Escaneo del boleto -----

    @Test
    void laSucursalEncuentraLaVentaEscaneandoElCodigoDelBoleto() {
        UUID ventaId = venderEnMostrador(1, 2);

        ResponseEntity<String> respuesta = get(
                VENTAS + "/boleto?codigo=" + codigo(ventaId), tokenCajero(xela));

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<List<String>>json(respuesta, "$[*].ventaId"))
                .containsExactly(ventaId.toString());
        assertThat(PruebaIntegracion.<List<String>>json(respuesta, "$[*].codigo"))
                .containsExactly(codigo(ventaId));
        assertThat(PruebaIntegracion.<List<Integer>>json(respuesta, "$[0].boletos[*].numeroAsiento"))
                .containsExactly(1, 2);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$[0].viaje.origenNombre"))
                .isEqualTo("Quetzaltenango");
    }

    @Test
    void elCodigoTambienSirveConGuionesOConElIdCompleto() {
        UUID ventaId = venderEnMostrador(3);
        String conGuiones = codigo(ventaId).replaceAll("(.{4})(?=.)", "$1-");

        esStatus(get(VENTAS + "/boleto?codigo=" + conGuiones, tokenCajero(xela)), 200);
        esStatus(get(VENTAS + "/boleto?codigo=" + ventaId, tokenCajero(xela)), 200);
    }

    @Test
    void unCodigoIncompletoOSinVentaNoDevuelveNada() {
        esError(get(VENTAS + "/boleto?codigo=A1B2-C3", tokenCajero(xela)), 400, "CODIGO_INVALIDO");
        esError(get(VENTAS + "/boleto?codigo=A1B2C3D4E5F6", tokenCajero(xela)), 404, "VENTA_NO_EXISTE");
    }

    @Test
    void unCajeroDeOtraSucursalNoVeElBoleto() {
        UUID ventaId = venderEnMostrador(4);

        esError(get(VENTAS + "/boleto?codigo=" + codigo(ventaId), tokenCajero(guate)), 404, "VENTA_NO_EXISTE");
    }

    // ----- Devolución del efectivo -----

    @Test
    void elEfectivoDeUnaCompraCanceladaSeDevuelveEnLaSucursal() {
        UUID ventaId = venderEnMostrador(5, 6);
        eliminarViaje("Bus averiado");
        assertThat(estadoVenta(ventaId)).isEqualTo("CANCELADA");
        int version = json(get(VENTAS + "/" + ventaId, tokenCajero(xela)), "$.version");

        ResponseEntity<String> respuesta = post(
                VENTAS + "/" + ventaId + "/reembolsar", tokenCajero(xela), reembolso(version));

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.estado")).isEqualTo("REEMBOLSADA");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.fechaReembolso")).isNotNull();
        // El motivo lleva el viaje escrito: su fila de vista_viajes ya no existe
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.motivoCancelacion"))
                .startsWith("Viaje eliminado · Quetzaltenango → Guatemala")
                .endsWith("Bus averiado");

        Map<String, Object> guardada = venta(ventaId);
        assertThat(guardada.get("estado")).isEqualTo("REEMBOLSADA");
        assertThat(guardada.get("fecha_reembolso")).isNotNull();
        assertThat(guardada.get("reembolsado_por")).isNotNull();

        // Notification avisa al cliente, que dejó su correo en la venta
        List<String> avisos = enOutbox("VentaReembolsada");
        assertThat(avisos).hasSize(1);
        assertThat(JsonPath.<String>read(avisos.get(0), "$.payload.ventaId")).isEqualTo(ventaId.toString());
        assertThat(JsonPath.<String>read(avisos.get(0), "$.payload.codigo")).isEqualTo(codigo(ventaId));
        assertThat(JsonPath.<String>read(avisos.get(0), "$.payload.clienteCorreo")).isEqualTo("ana@correo.com");
        assertThat(JsonPath.<String>read(avisos.get(0), "$.payload.metodoPago")).isEqualTo("EFECTIVO");
        assertThat(decimal(avisos.get(0), "$.payload.montoReembolsado")).isEqualByComparingTo("150.00");
        assertThat(JsonPath.<List<Integer>>read(avisos.get(0), "$.payload.asientos")).containsExactly(5, 6);
    }

    @Test
    void noSeDevuelveDosVecesLaMismaCompra() {
        UUID ventaId = venderEnMostrador(7);
        eliminarViaje("Bus averiado");
        int version = json(get(VENTAS + "/" + ventaId, tokenCajero(xela)), "$.version");
        esStatus(post(VENTAS + "/" + ventaId + "/reembolsar", tokenCajero(xela), reembolso(version)), 200);

        int nueva = json(get(VENTAS + "/" + ventaId, tokenCajero(xela)), "$.version");
        esError(post(VENTAS + "/" + ventaId + "/reembolsar", tokenCajero(xela), reembolso(nueva)),
                422, "VENTA_YA_REEMBOLSADA");

        assertThat(enOutbox("VentaReembolsada")).hasSize(1);
    }

    @Test
    void unaCompraConfirmadaNoSeReembolsa() {
        UUID ventaId = venderEnMostrador(8);
        int version = json(get(VENTAS + "/" + ventaId, tokenCajero(xela)), "$.version");

        esError(post(VENTAS + "/" + ventaId + "/reembolsar", tokenCajero(xela), reembolso(version)),
                422, "ESTADO_INVALIDO");
        assertThat(estadoVenta(ventaId)).isEqualTo("CONFIRMADA");
    }

    @Test
    void unaCompraEnLineaNoSeDevuelveEnEfectivoSinoPorLaCartera() {
        UUID ventaId = comprarYConfirmarEnLinea(9);
        eliminarViaje("Bus averiado");
        assertThat(estadoVenta(ventaId)).isEqualTo("CANCELADA");
        int reembolsosPedidos = enOutbox("ReembolsoCarteraSolicitado").size();
        int version = json(get(VENTAS + "/" + ventaId, tokenAdminSucursal(xela)), "$.version");

        ResponseEntity<String> respuesta = post(
                VENTAS + "/" + ventaId + "/reembolsar", tokenAdminSucursal(xela), reembolso(version));

        esStatus(respuesta, 200);
        // Sigue CANCELADA: el dinero lo devuelve Wallet, que responde CarteraReembolsada
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.estado")).isEqualTo("CANCELADA");
        assertThat(enOutbox("ReembolsoCarteraSolicitado")).hasSize(reembolsosPedidos + 1);
        assertThat(enOutbox("VentaReembolsada")).isEmpty();

        publicarEvento(WALLET_EVENTOS, "CarteraReembolsada", cliente, null,
                movimientoCartera("VENTA", ventaId, cliente, "75.00"));
        await().atMost(ESPERA).untilAsserted(() ->
                assertThat(estadoVenta(ventaId)).isEqualTo("REEMBOLSADA"));
        assertThat(venta(ventaId).get("fecha_reembolso")).isNotNull();
        assertThat(venta(ventaId).get("reembolsado_por")).isNull();      // no lo entregó ningún cajero
    }

    @Test
    void unCajeroDeOtraSucursalNoDevuelveElDineroDeEstaVenta() {
        UUID ventaId = venderEnMostrador(10);
        eliminarViaje("Bus averiado");
        int version = json(get(VENTAS + "/" + ventaId, tokenCajero(xela)), "$.version");

        esError(post(VENTAS + "/" + ventaId + "/reembolsar", tokenCajero(guate), reembolso(version)),
                403, "SUCURSAL_AJENA");
        assertThat(estadoVenta(ventaId)).isEqualTo("CANCELADA");
    }

    @Test
    void unClienteNoPuedeReembolsarseSolo() {
        UUID ventaId = venderEnMostrador(11);
        eliminarViaje("Bus averiado");

        esError(post(VENTAS + "/" + ventaId + "/reembolsar", tokenCliente(cliente), reembolso(1)),
                403, "ROL_INSUFICIENTE");
        assertThat(estadoVenta(ventaId)).isEqualTo("CANCELADA");
    }
}
