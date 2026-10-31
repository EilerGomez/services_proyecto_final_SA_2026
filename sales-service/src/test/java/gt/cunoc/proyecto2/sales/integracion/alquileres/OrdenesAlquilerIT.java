package gt.cunoc.proyecto2.sales.integracion.alquileres;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import gt.cunoc.proyecto2.sales.integracion.soporte.PruebaIntegracion;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class OrdenesAlquilerIT extends PruebaIntegracion {

    private static final String ORDENES = "/sales/ordenes-alquiler";
    private static final String NIT = "1234567-K";

    private UUID xela;
    private UUID guate;
    private UUID cliente;

    @BeforeEach
    void datos() {
        xela = UUID.randomUUID();
        guate = UUID.randomUUID();
        cliente = UUID.randomUUID();
    }

    private static String pago(LocalDate fecha) {
        return "{\"fechaPago\":\"%s\"}".formatted(fecha);
    }

    private static String pagoCartera(UUID ordenId) {
        return ORDENES + "/" + ordenId + "/pago-cartera";
    }

    private static String pagoEfectivo(UUID ordenId) {
        return ORDENES + "/" + ordenId + "/pago-efectivo";
    }

    private static int versionDelEvento(String sobre) {
        return JsonPath.<Number>read(sobre, "$.aggregateVersion").intValue();
    }

    private int enOutbox(String tipoEvento) {
        Integer total = jdbc.queryForObject(
                "SELECT count(*) FROM eventos_salida WHERE tipo_evento = ?",
                Integer.class, tipoEvento);
        return total == null ? 0 : total;
    }

    private void creadaEl(UUID ordenId, String instante) {
        jdbc.update(
                "UPDATE ordenes_alquiler SET fecha_creacion = ? WHERE id = ?",
                OffsetDateTime.parse(instante), ordenId);
    }

    private void cambiarOrden(
            UUID ordenId, String estado,
            String metodoPago, String motivoRechazo) {
        jdbc.update("""
                UPDATE ordenes_alquiler
                   SET estado = ?, metodo_pago = ?,
                       fecha_pago = ?, motivo_rechazo = ?
                 WHERE id = ?
                """, estado, metodoPago, FECHA_PAGO, motivoRechazo, ordenId);
    }

    @Test
    void elPersonalListaLasOrdenesDeSuSucursalPorEstado() {
        UUID pendiente = insertarOrden(xela, cliente, NIT);
        UUID pagada = insertarOrden(xela, null, "CF");
        UUID deGuate = insertarOrden(guate, null, "CF");

        creadaEl(pendiente, "2026-10-01T10:00:00-06:00");
        creadaEl(pagada, "2026-10-02T10:00:00-06:00");
        creadaEl(deGuate, "2026-10-03T10:00:00-06:00");
        cambiarOrden(pagada, "PAGADA", "EFECTIVO", null);

        ResponseEntity<String> deXela = get(ORDENES, tokenCajero(xela));
        ResponseEntity<String> pagadas = get(
                ORDENES + "?estado=PAGADA", tokenAdminSucursal(xela));
        ResponseEntity<String> pendientes = get(
                ORDENES + "?estado=PENDIENTE_PAGO&sucursalId=" + xela,
                tokenCajero(xela));
        ResponseEntity<String> todas = get(ORDENES, tokenAdminSistema());
        ResponseEntity<String> soloGuate = get(
                ORDENES + "?sucursalId=" + guate, tokenAdminSistema());

        esStatus(deXela, 200);

        assertThat(PruebaIntegracion.<List<String>>json(deXela, "$[*].ordenId"))
                .containsExactly(pagada.toString(), pendiente.toString());
        assertThat(PruebaIntegracion.<String>json(deXela, "$[1].clienteNit"))
                .isEqualTo(NIT);
        assertThat(PruebaIntegracion.<String>json(deXela, "$[1].sucursalId"))
                .isEqualTo(xela.toString());
        assertThat(PruebaIntegracion.<String>json(deXela, "$[1].origen"))
                .isEqualTo("Quetzaltenango");
        assertThat(decimal(deXela, "$[1].monto"))
                .isEqualByComparingTo("2600.00");
        assertThat(PruebaIntegracion.<String>json(deXela, "$[1].metodoPago"))
                .isNull();
        assertThat(PruebaIntegracion.<Integer>json(deXela, "$[1].version"))
                .isZero();
        assertThat(PruebaIntegracion.<String>json(deXela, "$[0].metodoPago"))
                .isEqualTo("EFECTIVO");

        assertThat(PruebaIntegracion.<List<String>>json(
                pagadas, "$[*].ordenId"))
                .containsExactly(pagada.toString());
        assertThat(PruebaIntegracion.<List<String>>json(
                pendientes, "$[*].ordenId"))
                .containsExactly(pendiente.toString());
        assertThat(PruebaIntegracion.<List<String>>json(todas, "$[*].ordenId"))
                .containsExactly(
                        deGuate.toString(), pagada.toString(), pendiente.toString());
        assertThat(PruebaIntegracion.<List<String>>json(
                soloGuate, "$[*].ordenId"))
                .containsExactly(deGuate.toString());
    }

    @Test
    void elClienteVeSoloSusOrdenes() {
        UUID primera = insertarOrden(xela, cliente, NIT);
        UUID segunda = insertarOrden(guate, cliente, NIT);
        insertarOrden(xela, UUID.randomUUID(), "999999-9");

        creadaEl(primera, "2026-10-01T10:00:00-06:00");
        creadaEl(segunda, "2026-10-08T10:00:00-06:00");

        ResponseEntity<String> respuesta =
                get(ORDENES + "/mias", tokenCliente(cliente));
        ResponseEntity<String> sinOrdenes =
                get(ORDENES + "/mias", tokenCliente(UUID.randomUUID()));

        esStatus(respuesta, 200);

        assertThat(PruebaIntegracion.<List<String>>json(
                respuesta, "$[*].ordenId"))
                .containsExactly(segunda.toString(), primera.toString());
        assertThat(PruebaIntegracion.<List<Object>>json(sinOrdenes, "$"))
                .isEmpty();

        esError(get(ORDENES + "/mias", tokenCajero(xela)),
                403, "ROL_INSUFICIENTE");
    }

    /**
     * Un alquiler pedido en ventanilla deja la orden sin cuenta enlazada. Si el NIT es el del
     * cliente, "Mis órdenes" se la muestra igual —es su orden de pago— sin importar mayúsculas.
     */
    @Test
    void elClienteVeLaOrdenQueQuedoASuNitSinCuenta() {
        insertarCliente(cliente, NIT, "Colegio San José", "colegio@correo.com");
        UUID conCuenta = insertarOrden(xela, cliente, NIT);
        UUID sinCuenta = insertarOrden(xela, null, NIT.toLowerCase(Locale.ROOT));
        UUID deOtroNit = insertarOrden(xela, null, "999999-9");

        ResponseEntity<String> respuesta = get(ORDENES + "/mias", tokenCliente(cliente));

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<List<String>>json(respuesta, "$[*].ordenId"))
                .containsExactlyInAnyOrder(conCuenta.toString(), sinCuenta.toString())
                .doesNotContain(deOtroNit.toString());
    }

    @Test
    void elDetalleLoVenSuClienteElPersonalDeLaSucursalYElAdminDelSistema() {
        UUID orden = insertarOrden(xela, cliente, NIT);
        String detalle = ORDENES + "/" + orden;

        ResponseEntity<String> delCliente = get(detalle, tokenCliente(cliente));

        esStatus(delCliente, 200);

        assertThat(PruebaIntegracion.<String>json(delCliente, "$.ordenId"))
                .isEqualTo(orden.toString());
        assertThat(PruebaIntegracion.<String>json(delCliente, "$.estado"))
                .isEqualTo("PENDIENTE_PAGO");
        assertThat(PruebaIntegracion.<String>json(delCliente, "$.destino"))
                .isEqualTo("Antigua Guatemala");
        assertThat(PruebaIntegracion.<String>json(delCliente, "$.clienteCorreo"))
                .isEqualTo("colegio@correo.com");

        esStatus(get(detalle, tokenCajero(xela)), 200);
        esStatus(get(detalle, tokenAdminSucursal(xela)), 200);
        esStatus(get(detalle, tokenAdminSistema()), 200);

        esError(get(detalle, tokenCliente(UUID.randomUUID())),
                403, "ORDEN_AJENA");
        esError(get(detalle, tokenCajero(guate)),
                403, "ORDEN_AJENA");
        esError(get(detalle, tokenAdminSucursal(guate)),
                403, "ORDEN_AJENA");
        esError(get(ORDENES + "/" + UUID.randomUUID(), tokenAdminSistema()),
                404, "ORDEN_NO_EXISTE");
        esError(get(detalle, null),
                401, "NO_AUTENTICADO");
    }

    @Test
    void pagarConCarteraQuedaEnProcesoYPideElDebito() {
        UUID orden = insertarOrden(xela, cliente, NIT);

        ResponseEntity<String> respuesta = post(
                pagoCartera(orden), tokenCliente(cliente), pago(FECHA_PAGO));

        esStatus(respuesta, 202);

        assertThat(PruebaIntegracion.<String>json(respuesta, "$.estado"))
                .isEqualTo("PENDIENTE_PAGO");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.metodoPago"))
                .isEqualTo("CARTERA");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.fechaPago"))
                .isEqualTo(FECHA_PAGO.toString());
        assertThat(PruebaIntegracion.<Integer>json(respuesta, "$.version"))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject(
                "SELECT metodo_pago FROM ordenes_alquiler WHERE id = ?",
                String.class, orden)).isEqualTo("CARTERA");

        assertThat(PruebaIntegracion.<String>json(
                get(ORDENES + "/" + orden, tokenCliente(cliente)),
                "$.metodoPago"))
                .isEqualTo("CARTERA");

        String evento = esperarEventos("DebitoCarteraSolicitado")
                .get("DebitoCarteraSolicitado");

        assertThat(JsonPath.<String>read(evento, "$.aggregateId"))
                .isEqualTo(orden.toString());
        assertThat(versionDelEvento(evento)).isEqualTo(1);
        assertThat(JsonPath.<String>read(evento, "$.businessDate"))
                .isEqualTo(FECHA_PAGO.toString());
        assertThat(JsonPath.<String>read(evento, "$.payload.referenciaTipo"))
                .isEqualTo("ALQUILER");
        assertThat(JsonPath.<String>read(evento, "$.payload.referenciaId"))
                .isEqualTo(orden.toString());
        assertThat(JsonPath.<String>read(evento, "$.payload.usuarioId"))
                .isEqualTo(cliente.toString());
        assertThat(decimal(evento, "$.payload.monto"))
                .isEqualByComparingTo("2600.00");
        assertThat(JsonPath.<String>read(evento, "$.payload.fechaMovimiento"))
                .isEqualTo(FECHA_PAGO.toString());
        assertThat(jdbc.queryForObject(
                "SELECT tipo_agregado FROM eventos_salida WHERE agregado_id = ?",
                String.class, orden)).isEqualTo("OrdenAlquiler");

        esError(post(
                pagoCartera(orden), tokenCliente(cliente), pago(FECHA_PAGO)),
                409, "PAGO_EN_PROCESO");

        assertThat(enOutbox("DebitoCarteraSolicitado")).isEqualTo(1);
    }

    @Test
    void pagarEnEfectivoDejaLaOrdenPagadaYPublicaAlquilerPagado() {
        UUID orden = insertarOrden(xela, null, "CF");
        UUID alquiler = jdbc.queryForObject(
                "SELECT alquiler_id FROM ordenes_alquiler WHERE id = ?",
                UUID.class, orden);

        ResponseEntity<String> respuesta = post(
                pagoEfectivo(orden), tokenCajero(xela), pago(FECHA_PAGO));

        esStatus(respuesta, 200);

        assertThat(PruebaIntegracion.<String>json(respuesta, "$.estado"))
                .isEqualTo("PAGADA");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.metodoPago"))
                .isEqualTo("EFECTIVO");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.fechaPago"))
                .isEqualTo(FECHA_PAGO.toString());
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.alquilerId"))
                .isEqualTo(alquiler.toString());
        assertThat(PruebaIntegracion.<Integer>json(respuesta, "$.version"))
                .isEqualTo(1);
        assertThat(estadoOrden(orden)).isEqualTo("PAGADA");

        String evento = esperarEventos("AlquilerPagado").get("AlquilerPagado");

        assertThat(JsonPath.<String>read(evento, "$.aggregateId"))
                .isEqualTo(orden.toString());
        assertThat(versionDelEvento(evento)).isEqualTo(1);
        assertThat(JsonPath.<String>read(evento, "$.businessDate"))
                .isEqualTo(FECHA_PAGO.toString());
        assertThat(JsonPath.<String>read(evento, "$.payload.ordenId"))
                .isEqualTo(orden.toString());
        assertThat(JsonPath.<String>read(evento, "$.payload.alquilerId"))
                .isEqualTo(alquiler.toString());
        assertThat(JsonPath.<String>read(evento, "$.payload.sucursalId"))
                .isEqualTo(xela.toString());
        assertThat(decimal(evento, "$.payload.monto"))
                .isEqualByComparingTo("2600.00");
        assertThat(decimal(evento, "$.payload.montoTotal"))
                .isEqualByComparingTo("2600.00");
        assertThat(JsonPath.<String>read(evento, "$.payload.metodoPago"))
                .isEqualTo("EFECTIVO");
        assertThat(JsonPath.<String>read(evento, "$.payload.fechaPago"))
                .isEqualTo(FECHA_PAGO.toString());
        assertThat(JsonPath.<String>read(evento, "$.payload.origen"))
                .isEqualTo("Quetzaltenango");
        assertThat(jdbc.queryForObject("""
                SELECT tipo_agregado FROM eventos_salida
                 WHERE tipo_evento = 'AlquilerPagado'
                """, String.class)).isEqualTo("OrdenAlquiler");
    }
}