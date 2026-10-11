package gt.cunoc.proyecto2.sales.integracion.historial;

import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.sales.integracion.soporte.PruebaIntegracion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/** S7 y S8: historial de compras de boletos (en línea y en mostrador) y alquileres de un NIT. */
class HistorialIT extends PruebaIntegracion {

    private static final String MIO = "/sales/historial/mio";
    private static final String POR_NIT = "/sales/historial?nit=";
    private static final String NIT = "1234567-K";
    private static final BigDecimal PRECIO = new BigDecimal("75.00");

    private UUID xela;
    private UUID viaje;
    private UUID cliente;

    @BeforeEach
    void datos() {
        xela = UUID.randomUUID();
        viaje = insertarViaje(xela);
        cliente = UUID.randomUUID();
    }

    private static OffsetDateTime fecha(ResponseEntity<String> respuesta, String ruta) {
        return OffsetDateTime.parse(PruebaIntegracion.<String>json(respuesta, ruta));
    }

    /**
     * Venta CONFIRMADA directa en la BD (CHECK de V5 y V8): con cliente es EN_LINEA con CARTERA; sin cliente,
     * PRESENCIAL en EFECTIVO con cajero. Lleva boletos ACTIVO y asientos VENDIDO.
     */
    private UUID insertarVenta(UUID viajeId, UUID clienteUsuarioId, String nit, String nombre, LocalDate fechaPago,
                               Integer... asientos) {
        UUID id = UUID.randomUUID();
        boolean enLinea = clienteUsuarioId != null;
        jdbc.update("""
                INSERT INTO ventas (id, viaje_id, canal, cliente_usuario_id, cliente_nit, cliente_nombre, sucursal_id,
                                    cajero_usuario_id, metodo_pago, monto_total, cantidad_boletos, fecha_pago, estado)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'CONFIRMADA')""",
                id, viajeId, enLinea ? "EN_LINEA" : "PRESENCIAL", clienteUsuarioId, nit, nombre, xela,
                enLinea ? null : UUID.randomUUID(), enLinea ? "CARTERA" : "EFECTIVO",
                PRECIO.multiply(BigDecimal.valueOf(asientos.length)), asientos.length, fechaPago);
        for (Integer numero : asientos) {
            jdbc.update("INSERT INTO boletos (venta_id, viaje_id, numero_asiento, precio) VALUES (?, ?, ?, 75.00)",
                    id, viajeId, numero);
            jdbc.update("UPDATE asientos_viaje SET estado = 'VENDIDO', venta_id = ? WHERE viaje_id = ? AND numero_asiento = ?",
                    id, viajeId, numero);
        }
        return id;
    }

    /** Fija fecha_creacion para que el orden de las órdenes no dependa del reloj. */
    private void creadaEl(UUID ordenId, String instante) {
        jdbc.update("UPDATE ordenes_alquiler SET fecha_creacion = ? WHERE id = ?", OffsetDateTime.parse(instante), ordenId);
    }

    // ----- S7 -----

    @Test
    void elClienteVeSusComprasYAlquileresPorElNitDeSuPerfil() {
        insertarCliente(cliente, NIT, "Ana Lopez", "ana@correo.com");
        UUID otroViaje = insertarViaje(xela);
        UUID enLinea = insertarVenta(viaje, cliente, NIT, "Ana Lopez", LocalDate.of(2026, 11, 1), 4, 2);
        UUID enMostrador = insertarVenta(otroViaje, null, NIT, "Ana L.", LocalDate.of(2026, 11, 5), 7);
        insertarVenta(viaje, null, "999999-9", "Otro", LocalDate.of(2026, 11, 6), 9);
        jdbc.update("DELETE FROM vista_viajes WHERE viaje_id = ?", otroViaje);       // la venta se conserva
        UUID primera = insertarOrden(xela, cliente, NIT);
        UUID segunda = insertarOrden(xela, null, NIT);                              // pedida en mostrador con su NIT
        insertarOrden(xela, null, "999999-9");
        creadaEl(primera, "2026-10-01T10:00:00-06:00");
        creadaEl(segunda, "2026-10-05T10:00:00-06:00");

        ResponseEntity<String> respuesta = get(MIO, tokenCliente(cliente));

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.nit")).isEqualTo(NIT);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.nombre")).isEqualTo("Ana Lopez");    // el del perfil
        // ventas: la fecha de pago más reciente primero; incluye la compra en mostrador con su NIT
        assertThat(PruebaIntegracion.<List<String>>json(respuesta, "$.ventas[*].ventaId"))
                .containsExactly(enMostrador.toString(), enLinea.toString());
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.ventas[0].canal")).isEqualTo("PRESENCIAL");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.ventas[0].origenNombre")).isNull();   // viaje eliminado
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.ventas[0].fechaSalida")).isNull();
        assertThat(PruebaIntegracion.<List<Integer>>json(respuesta, "$.ventas[0].asientos")).containsExactly(7);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.ventas[1].canal")).isEqualTo("EN_LINEA");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.ventas[1].origenNombre")).isEqualTo("Quetzaltenango");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.ventas[1].destinoNombre")).isEqualTo("Guatemala");
        assertThat(fecha(respuesta, "$.ventas[1].fechaSalida")).isEqualTo(SALIDA);
        assertThat(PruebaIntegracion.<List<Integer>>json(respuesta, "$.ventas[1].asientos")).containsExactly(2, 4);
        assertThat(decimal(respuesta, "$.ventas[1].montoTotal")).isEqualByComparingTo("150.00");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.ventas[1].fechaPago")).isEqualTo("2026-11-01");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.ventas[1].estado")).isEqualTo("CONFIRMADA");
        // alquileres: los más recientes primero
        assertThat(PruebaIntegracion.<List<String>>json(respuesta, "$.alquileres[*].ordenId"))
                .containsExactly(segunda.toString(), primera.toString());
        assertThat(decimal(respuesta, "$.alquileres[1].monto")).isEqualByComparingTo("2600.00");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.alquileres[1].origen")).isEqualTo("Quetzaltenango");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.alquileres[1].destino")).isEqualTo("Antigua Guatemala");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.alquileres[1].estado")).isEqualTo("PENDIENTE_PAGO");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.alquileres[1].metodoPago")).isNull();
    }

    @Test
    void unClienteSinComprasVeSuHistorialVacio() {
        insertarCliente(cliente, "7654321", "Luis Perez", "luis@correo.com");
        insertarVenta(viaje, null, NIT, "Ana Lopez", FECHA_PAGO, 1);

        ResponseEntity<String> respuesta = get(MIO, tokenCliente(cliente));

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.nit")).isEqualTo("7654321");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.nombre")).isEqualTo("Luis Perez");
        assertThat(PruebaIntegracion.<List<Object>>json(respuesta, "$.ventas")).isEmpty();
        assertThat(PruebaIntegracion.<List<Object>>json(respuesta, "$.alquileres")).isEmpty();
    }

    @Test
    void miHistorialSinPerfilOConOtroRolFalla() {
        esError(get(MIO, tokenCliente(cliente)), 422, "CLIENTE_SIN_PERFIL");
        esError(get(MIO, tokenCajero(xela)), 403, "ROL_INSUFICIENTE");
        esError(get(MIO, null), 401, "NO_AUTENTICADO");
    }

    // ----- S8 -----

    @Test
    void elPersonalBuscaElHistorialDeUnNitConCuenta() {
        insertarCliente(cliente, NIT, "Ana Lopez", "ana@correo.com");
        UUID enLinea = insertarVenta(viaje, cliente, NIT, "Ana Lopez", LocalDate.of(2026, 11, 1), 1);
        UUID enMostrador = insertarVenta(viaje, null, NIT, "Ana L.", LocalDate.of(2026, 11, 2), 2);
        insertarVenta(viaje, null, "999999-9", "Otro", LocalDate.of(2026, 11, 3), 3);
        UUID orden = insertarOrden(xela, cliente, NIT);

        ResponseEntity<String> delCajero = get(POR_NIT + "1234567-k", tokenCajero(xela));        // se normaliza

        esStatus(delCajero, 200);
        assertThat(PruebaIntegracion.<String>json(delCajero, "$.nit")).isEqualTo(NIT);
        assertThat(PruebaIntegracion.<String>json(delCajero, "$.nombre")).isEqualTo("Ana Lopez");
        assertThat(PruebaIntegracion.<List<String>>json(delCajero, "$.ventas[*].ventaId"))
                .containsExactly(enMostrador.toString(), enLinea.toString());
        assertThat(PruebaIntegracion.<List<String>>json(delCajero, "$.alquileres[*].ordenId"))
                .containsExactly(orden.toString());
        esStatus(get(POR_NIT + NIT, tokenAdminSucursal(UUID.randomUUID())), 200);
        esStatus(get(POR_NIT + NIT, tokenAdminSistema()), 200);
    }

    @Test
    void sinCuentaElNombreEsElDeLaCompraMasReciente() {
        insertarVenta(viaje, null, "5555555", "Pedro R.", LocalDate.of(2026, 11, 1), 1);
        UUID reciente = insertarVenta(viaje, null, "5555555", "Pedro Ruiz", LocalDate.of(2026, 11, 9), 2);

        ResponseEntity<String> respuesta = get(POR_NIT + "5555555", tokenCajero(xela));
        ResponseEntity<String> sinNada = get(POR_NIT + "1111111", tokenCajero(xela));

        esStatus(respuesta, 200);
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.nombre")).isEqualTo("Pedro Ruiz");
        assertThat(PruebaIntegracion.<String>json(respuesta, "$.ventas[0].ventaId")).isEqualTo(reciente.toString());
        assertThat(PruebaIntegracion.<List<Object>>json(respuesta, "$.alquileres")).isEmpty();
        esStatus(sinNada, 200);
        assertThat(PruebaIntegracion.<String>json(sinNada, "$.nombre")).isNull();
        assertThat(PruebaIntegracion.<List<Object>>json(sinNada, "$.ventas")).isEmpty();
        assertThat(PruebaIntegracion.<List<Object>>json(sinNada, "$.alquileres")).isEmpty();
    }

    @Test
    void elConsumidorFinalNoTieneHistorialYElNitEsObligatorio() {
        insertarVenta(viaje, null, "CF", "Consumidor final", FECHA_PAGO, 1);

        esError(get(POR_NIT + "CF", tokenCajero(xela)), 400, "NIT_CONSUMIDOR_FINAL");
        esError(get(POR_NIT + "cf", tokenCajero(xela)), 400, "NIT_CONSUMIDOR_FINAL");
        esError(get("/sales/historial", tokenCajero(xela)), 400, "PARAMETRO_INVALIDO");
        esError(get(POR_NIT + NIT, tokenCliente(cliente)), 403, "ROL_INSUFICIENTE");
        esError(get(POR_NIT + NIT, null), 401, "NO_AUTENTICADO");
    }
}
