package gt.cunoc.proyecto2.sales.eventos;

import static gt.cunoc.proyecto2.sales.DatosPrueba.FECHA_PAGO;
import static gt.cunoc.proyecto2.sales.DatosPrueba.SALIDA;
import static gt.cunoc.proyecto2.sales.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import gt.cunoc.proyecto2.comun.eventos.publicador.PublicadorEventos;
import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.eventos.publicados.alquileres.AlquilerPagado;
import gt.cunoc.proyecto2.sales.eventos.publicados.cartera.DebitoCarteraSolicitado;
import gt.cunoc.proyecto2.sales.eventos.publicados.cartera.ReembolsoCarteraSolicitado;
import gt.cunoc.proyecto2.sales.eventos.publicados.ventas.BoletoVendido;
import gt.cunoc.proyecto2.sales.eventos.publicados.ventas.VentaCancelada;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.CanalVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.MetodoPago;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/** Cada método publica con su agregado, id, version, nombre de evento, payload y businessDate. */
class PublicadorEventosSalesTest {

    private static final LocalDate FECHA_CANCELACION = LocalDate.of(2026, 11, 2);
    private static final LocalDate FECHA_REEMBOLSO = LocalDate.of(2026, 11, 3);

    private final PublicadorEventos publicador = mock(PublicadorEventos.class);
    private final PublicadorEventosSales eventos = new PublicadorEventosSales(publicador);

    private final UUID clienteId = UUID.randomUUID();
    private final VistaViajeEntidad viaje = DatosPrueba.vistaViaje();

    /** Verifica tipo de agregado, id, version, nombre del evento y businessDate; devuelve el payload. */
    private Object publicado(String agregado, UUID id, Long version, String evento, LocalDate fecha) {
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(publicador).publicar(eq(agregado), eq(id), eq(version), eq(evento), payload.capture(), eq(fecha));
        verifyNoMoreInteractions(publicador);
        return payload.getValue();
    }

    // ----- Saga con Identity -----

    @Test
    void debitoVentaConLaFechaDePago() {
        VentaEntidad venta = DatosPrueba.ventaEnLinea(viaje, clienteId, 2);

        eventos.debitoVenta(venta);

        Object payload = publicado("Venta", venta.getId(), 0L, "DebitoCarteraSolicitado", FECHA_PAGO);
        assertThat(payload).isEqualTo(DebitoCarteraSolicitado.deVenta(venta));
        assertThat(payload).isInstanceOfSatisfying(DebitoCarteraSolicitado.class, e -> {
            assertThat(e.referenciaTipo()).isEqualTo(ReferenciaCartera.VENTA);
            assertThat(e.referenciaId()).isEqualTo(venta.getId());
            assertThat(e.usuarioId()).isEqualTo(clienteId);
            assertThat(e.monto()).isEqualByComparingTo("150.00");
            assertThat(e.fechaMovimiento()).isEqualTo(FECHA_PAGO);
        });
    }

    @Test
    void debitoVentaSinVersionPublicaVersionNula() {
        // recién creada, antes de guardarse: version null
        VentaEntidad venta = VentaEntidad.registrarEnLinea(viaje, DatosPrueba.clienteConCuenta(clienteId), 1,
                FECHA_PAGO);

        eventos.debitoVenta(venta);

        publicado("Venta", venta.getId(), null, "DebitoCarteraSolicitado", FECHA_PAGO);
    }

    @Test
    void debitoOrdenConLaFechaDePagoElegida() {
        OrdenAlquilerEntidad orden = DatosPrueba.orden(clienteId);
        orden.solicitarPagoConCartera(FECHA_PAGO);
        DatosPrueba.conVersion(orden, 1);

        eventos.debitoOrden(orden);

        Object payload = publicado("OrdenAlquiler", orden.getId(), 1L, "DebitoCarteraSolicitado", FECHA_PAGO);
        assertThat(payload).isEqualTo(DebitoCarteraSolicitado.deOrden(orden));
        assertThat(payload).isInstanceOfSatisfying(DebitoCarteraSolicitado.class, e -> {
            assertThat(e.referenciaTipo()).isEqualTo(ReferenciaCartera.ALQUILER);
            assertThat(e.referenciaId()).isEqualTo(orden.getId());
            assertThat(e.usuarioId()).isEqualTo(clienteId);
            assertThat(e.monto()).isEqualByComparingTo("2600.00");
            assertThat(e.fechaMovimiento()).isEqualTo(FECHA_PAGO);
        });
    }

    @Test
    void reembolsoVentaConLaFechaIndicada() {
        VentaEntidad venta = DatosPrueba.ventaEnLinea(viaje, clienteId, 2);
        venta.rechazar(MotivoSaga.TIEMPO_AGOTADO.name());
        DatosPrueba.conVersion(venta, 1);

        eventos.reembolsoVenta(venta, MotivoSaga.DEBITO_TARDIO, FECHA_REEMBOLSO);

        Object payload = publicado("Venta", venta.getId(), 1L, "ReembolsoCarteraSolicitado", FECHA_REEMBOLSO);
        assertThat(payload).isEqualTo(ReembolsoCarteraSolicitado.deVenta(venta, MotivoSaga.DEBITO_TARDIO,
                FECHA_REEMBOLSO));
        assertThat(payload).isInstanceOfSatisfying(ReembolsoCarteraSolicitado.class, e -> {
            assertThat(e.referenciaTipo()).isEqualTo(ReferenciaCartera.VENTA);
            assertThat(e.usuarioId()).isEqualTo(clienteId);
            assertThat(e.monto()).isEqualByComparingTo("150.00");
            assertThat(e.motivo()).isEqualTo(MotivoSaga.DEBITO_TARDIO);
            assertThat(e.fechaMovimiento()).isEqualTo(FECHA_REEMBOLSO);
        });
    }

    @Test
    void reembolsoOrdenConLaFechaIndicada() {
        OrdenAlquilerEntidad orden = DatosPrueba.orden(clienteId);
        orden.solicitarPagoConCartera(FECHA_PAGO);
        orden.pagarEnEfectivo(FECHA_PAGO);
        DatosPrueba.conVersion(orden, 2);

        eventos.reembolsoOrden(orden, MotivoSaga.DEBITO_TARDIO, FECHA_REEMBOLSO);

        Object payload = publicado("OrdenAlquiler", orden.getId(), 2L, "ReembolsoCarteraSolicitado",
                FECHA_REEMBOLSO);
        assertThat(payload).isEqualTo(ReembolsoCarteraSolicitado.deOrden(orden, MotivoSaga.DEBITO_TARDIO,
                FECHA_REEMBOLSO));
        assertThat(payload).isInstanceOfSatisfying(ReembolsoCarteraSolicitado.class, e -> {
            assertThat(e.referenciaTipo()).isEqualTo(ReferenciaCartera.ALQUILER);
            assertThat(e.referenciaId()).isEqualTo(orden.getId());
            assertThat(e.monto()).isEqualByComparingTo("2600.00");
            assertThat(e.motivo()).isEqualTo(MotivoSaga.DEBITO_TARDIO);
        });
    }

    // ----- Ventas -----

    @Test
    void boletoVendidoConLaFechaDePago() {
        VentaEntidad venta = DatosPrueba.conVersion(DatosPrueba.ventaEnLineaConfirmada(viaje, clienteId, 2), 1);
        List<BoletoEntidad> boletos = DatosPrueba.boletos(venta, 4, 3);

        eventos.boletoVendido(venta, boletos, viaje);

        Object payload = publicado("Venta", venta.getId(), 1L, "BoletoVendido", FECHA_PAGO);
        assertThat(payload).isEqualTo(BoletoVendido.desde(venta, boletos, viaje));
        assertThat(payload).isInstanceOfSatisfying(BoletoVendido.class, e -> {
            assertThat(e.ventaId()).isEqualTo(venta.getId());
            assertThat(e.viajeId()).isEqualTo(viaje.getViajeId());
            assertThat(e.rutaId()).isEqualTo(viaje.getRutaId());
            assertThat(e.busPlaca()).isEqualTo("P-123ABC");
            assertThat(e.sucursalId()).isEqualTo(XELA);
            assertThat(e.canal()).isEqualTo(CanalVenta.EN_LINEA);
            assertThat(e.asientos()).containsExactly(3, 4);
            assertThat(e.cantidadBoletos()).isEqualTo(2);
            assertThat(e.montoTotal()).isEqualByComparingTo("150.00");
            assertThat(e.metodoPago()).isEqualTo(MetodoPago.CARTERA);
            assertThat(e.fechaPago()).isEqualTo(FECHA_PAGO);
            assertThat(e.fechaSalida()).isAtSameInstantAs(SALIDA);
        });
    }

    @Test
    void ventaCanceladaConLaFechaDeCancelacion() {
        VentaEntidad venta = DatosPrueba.ventaMostrador(viaje, 2);
        venta.cancelar("Viaje suspendido", FECHA_CANCELACION);
        DatosPrueba.conVersion(venta, 2);
        List<BoletoEntidad> boletos = DatosPrueba.boletos(venta, 10, 11);

        eventos.ventaCancelada(venta, boletos, viaje);

        Object payload = publicado("Venta", venta.getId(), 2L, "VentaCancelada", FECHA_CANCELACION);
        assertThat(payload).isEqualTo(VentaCancelada.desde(venta, boletos, viaje));
        assertThat(payload).isInstanceOfSatisfying(VentaCancelada.class, e -> {
            assertThat(e.asientos()).containsExactly(10, 11);
            assertThat(e.montoAnulado()).isEqualByComparingTo("150.00");
            assertThat(e.metodoPago()).isEqualTo(MetodoPago.EFECTIVO);
            assertThat(e.motivo()).isEqualTo("Viaje suspendido");
            assertThat(e.fechaCancelacion()).isEqualTo(FECHA_CANCELACION);
            assertThat(e.origenNombre()).isEqualTo("Quetzaltenango");
        });
    }

    @Test
    void ventaCanceladaSinViajeDejaNulosLosDatosDelViaje() {
        VentaEntidad venta = DatosPrueba.ventaEnLineaConfirmada(viaje, clienteId, 1);
        venta.cancelar("Viaje eliminado", FECHA_CANCELACION);
        DatosPrueba.conVersion(venta, 1);
        List<BoletoEntidad> boletos = DatosPrueba.boletos(venta, 5);

        eventos.ventaCancelada(venta, boletos, null);

        Object payload = publicado("Venta", venta.getId(), 1L, "VentaCancelada", FECHA_CANCELACION);
        assertThat(payload).isInstanceOfSatisfying(VentaCancelada.class, e -> {
            assertThat(e.origenNombre()).isNull();
            assertThat(e.destinoNombre()).isNull();
            assertThat(e.fechaSalida()).isNull();
            assertThat(e.cantidadBoletos()).isEqualTo(1);
        });
    }

}
