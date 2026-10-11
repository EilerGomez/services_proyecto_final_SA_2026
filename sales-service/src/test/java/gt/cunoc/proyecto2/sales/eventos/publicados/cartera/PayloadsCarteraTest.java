package gt.cunoc.proyecto2.sales.eventos.publicados.cartera;

import static gt.cunoc.proyecto2.sales.DatosPrueba.FECHA_PAGO;
import static org.assertj.core.api.Assertions.assertThat;

import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.eventos.MotivoSaga;
import gt.cunoc.proyecto2.sales.eventos.ReferenciaCartera;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PayloadsCarteraTest {

    private final UUID clienteId = UUID.randomUUID();

    // ----- DebitoCarteraSolicitado -----

    @Test
    void debitoDeUnaVentaCobraElMontoTotalEnLaFechaDePago() {
        VentaEntidad venta = DatosPrueba.ventaEnLinea(DatosPrueba.vistaViaje(), clienteId, 2);

        DebitoCarteraSolicitado debito = DebitoCarteraSolicitado.deVenta(venta);

        assertThat(debito.referenciaTipo()).isEqualTo(ReferenciaCartera.VENTA);
        assertThat(debito.referenciaId()).isEqualTo(venta.getId());
        assertThat(debito.usuarioId()).isEqualTo(clienteId);
        assertThat(debito.monto()).isEqualByComparingTo("150.00");
        assertThat(debito.fechaMovimiento()).isEqualTo(FECHA_PAGO);
    }

    @Test
    void debitoDeUnaOrdenCobraElMontoEnLaFechaElegida() {
        OrdenAlquilerEntidad orden = DatosPrueba.orden(clienteId);
        LocalDate fecha = FECHA_PAGO.plusDays(4);
        orden.solicitarPagoConCartera(fecha);

        DebitoCarteraSolicitado debito = DebitoCarteraSolicitado.deOrden(orden);

        assertThat(debito.referenciaTipo()).isEqualTo(ReferenciaCartera.ALQUILER);
        assertThat(debito.referenciaId()).isEqualTo(orden.getId());
        assertThat(debito.usuarioId()).isEqualTo(clienteId);
        assertThat(debito.monto()).isEqualByComparingTo("2600.00");
        assertThat(debito.fechaMovimiento()).isEqualTo(fecha);
    }

    // ----- ReembolsoCarteraSolicitado -----

    @Test
    void reembolsoDeUnaVentaDevuelveElMontoTotal() {
        VentaEntidad venta = DatosPrueba.ventaEnLineaConfirmada(DatosPrueba.vistaViaje(), clienteId, 3);
        LocalDate fecha = FECHA_PAGO.plusDays(2);

        ReembolsoCarteraSolicitado reembolso = ReembolsoCarteraSolicitado.deVenta(venta, MotivoSaga.VENTA_CANCELADA,
                fecha);

        assertThat(reembolso.referenciaTipo()).isEqualTo(ReferenciaCartera.VENTA);
        assertThat(reembolso.referenciaId()).isEqualTo(venta.getId());
        assertThat(reembolso.usuarioId()).isEqualTo(clienteId);
        assertThat(reembolso.monto()).isEqualByComparingTo("225.00");
        assertThat(reembolso.motivo()).isEqualTo(MotivoSaga.VENTA_CANCELADA);
        assertThat(reembolso.fechaMovimiento()).isEqualTo(fecha);
    }

    @Test
    void reembolsoDeUnaOrdenPorDebitoTardio() {
        OrdenAlquilerEntidad orden = DatosPrueba.orden(clienteId);
        orden.solicitarPagoConCartera(FECHA_PAGO);
        orden.pagarEnEfectivo(FECHA_PAGO);
        LocalDate fecha = FECHA_PAGO.plusDays(1);

        ReembolsoCarteraSolicitado reembolso = ReembolsoCarteraSolicitado.deOrden(orden, MotivoSaga.DEBITO_TARDIO,
                fecha);

        assertThat(reembolso.referenciaTipo()).isEqualTo(ReferenciaCartera.ALQUILER);
        assertThat(reembolso.referenciaId()).isEqualTo(orden.getId());
        assertThat(reembolso.usuarioId()).isEqualTo(clienteId);
        assertThat(reembolso.monto()).isEqualByComparingTo("2600.00");
        assertThat(reembolso.motivo()).isEqualTo(MotivoSaga.DEBITO_TARDIO);
        assertThat(reembolso.fechaMovimiento()).isEqualTo(fecha);
    }
}
