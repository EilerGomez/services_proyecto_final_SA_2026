package gt.cunoc.proyecto2.sales.servicios.pagos;

import static gt.cunoc.proyecto2.sales.DatosPrueba.FECHA_PAGO;
import static gt.cunoc.proyecto2.sales.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.eventos.MotivoSaga;
import gt.cunoc.proyecto2.sales.eventos.PublicadorEventosSales;
import gt.cunoc.proyecto2.sales.eventos.ReferenciaCartera;
import gt.cunoc.proyecto2.sales.modelos.alquileres.EstadoOrdenAlquiler;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.EstadoBoleto;
import gt.cunoc.proyecto2.sales.modelos.ventas.EstadoVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.MetodoPago;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import gt.cunoc.proyecto2.sales.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import gt.cunoc.proyecto2.sales.repositorios.alquileres.OrdenAlquilerRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.asientos.AsientoViajeRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.ventas.BoletoRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.ventas.VentaRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.viajes.VistaViajeRepositorio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.springframework.test.util.ReflectionTestUtils;

/** Saga de pago con cartera: CarteraDebitada, DebitoCarteraRechazado y CarteraReembolsada. */
class PagoCarteraServicioImplTest {

    private final VentaRepositorio ventas = mock(VentaRepositorio.class);
    private final BoletoRepositorio boletos = mock(BoletoRepositorio.class);
    private final AsientoViajeRepositorio asientos = mock(AsientoViajeRepositorio.class);
    private final VistaViajeRepositorio viajes = mock(VistaViajeRepositorio.class);
    private final OrdenAlquilerRepositorio ordenes = mock(OrdenAlquilerRepositorio.class);
    private final PublicadorEventosSales eventos = mock(PublicadorEventosSales.class);

    private final UUID clienteId = UUID.randomUUID();
    private final VistaViajeEntidad viaje = DatosPrueba.vistaViaje();

    private PagoCarteraServicioImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new PagoCarteraServicioImpl(ventas, boletos, asientos, viajes, ordenes, eventos);
        when(ventas.saveAndFlush(any(VentaEntidad.class))).thenAnswer(i -> i.getArgument(0));
        when(ordenes.saveAndFlush(any(OrdenAlquilerEntidad.class))).thenAnswer(i -> i.getArgument(0));
        when(boletos.saveAll(ArgumentMatchers.<BoletoEntidad>anyList())).thenAnswer(i -> i.getArgument(0));
    }

    /** Débito tardío: la fecha del reembolso es max(hoy en Guatemala, fecha de pago). */
    private static LocalDate fechaReembolso() {
        LocalDate hoy = LocalDate.now(ZoneId.of("America/Guatemala"));
        return hoy.isBefore(FECHA_PAGO) ? FECHA_PAGO : hoy;
    }

    /** La saga lee la venta con bloqueo (buscarParaActualizar). */
    private void bloqueable(VentaEntidad venta) {
        when(ventas.buscarParaActualizar(venta.getId())).thenReturn(Optional.of(venta));
    }

    private void existe(OrdenAlquilerEntidad orden) {
        when(ordenes.findById(orden.getId())).thenReturn(Optional.of(orden));
    }

    private VentaEntidad ventaPendiente(int asientosComprados) {
        return DatosPrueba.ventaEnLinea(viaje, clienteId, asientosComprados);
    }

    private VentaEntidad ventaRechazada() {
        VentaEntidad venta = ventaPendiente(2);
        venta.rechazar(MotivoSaga.TIEMPO_AGOTADO.name());
        return venta;
    }

    private VentaEntidad ventaCancelada() {
        VentaEntidad venta = DatosPrueba.ventaEnLineaConfirmada(viaje, clienteId, 2);
        venta.cancelar("Cambio de planes", FECHA_PAGO);
        return venta;
    }

    /** Orden con el cobro con cartera en curso (PENDIENTE_PAGO + CARTERA). */
    private OrdenAlquilerEntidad ordenCobrando() {
        OrdenAlquilerEntidad orden = DatosPrueba.orden(clienteId);
        orden.solicitarPagoConCartera(FECHA_PAGO);
        return orden;
    }

    /** Después de un CarteraDebitada que no debía hacer nada con la venta. */
    private void sinEfectosEnLaVenta(VentaEntidad venta, EstadoVenta estado) {
        assertThat(venta.getEstado()).isEqualTo(estado);
        verify(ventas, never()).saveAndFlush(any());
        verifyNoInteractions(asientos, boletos, viajes, ordenes, eventos);
    }

    // ===================== CarteraDebitada de una VENTA =====================

    @Test
    void carteraDebitadaDeVentaInexistenteNoHaceNada() {
        UUID ventaId = UUID.randomUUID();
        when(ventas.buscarParaActualizar(ventaId)).thenReturn(Optional.empty());

        servicio.carteraDebitada(ReferenciaCartera.VENTA, ventaId);

        verify(ventas).buscarParaActualizar(ventaId);
        verify(ventas, never()).findById(any());
        verify(ventas, never()).saveAndFlush(any());
        verifyNoInteractions(asientos, boletos, viajes, ordenes, eventos);
    }

    @Test
    void carteraDebitadaConfirmaLaVentaYEmiteBoletos() {
        VentaEntidad venta = ventaPendiente(2);
        bloqueable(venta);
        when(viajes.findById(viaje.getViajeId())).thenReturn(Optional.of(viaje));
        when(asientos.reservadosDeVenta(venta.getId())).thenReturn(List.of(3, 4));

        servicio.carteraDebitada(ReferenciaCartera.VENTA, venta.getId());

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.CONFIRMADA);
        InOrder orden = inOrder(asientos, ventas, boletos, eventos);
        orden.verify(asientos).confirmarReservas(venta.getId());
        orden.verify(ventas).saveAndFlush(venta);
        orden.verify(boletos).saveAll(ArgumentMatchers.<BoletoEntidad>anyList());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BoletoEntidad>> emitidos = ArgumentCaptor.forClass(List.class);
        orden.verify(eventos).boletoVendido(eq(venta), emitidos.capture(), eq(viaje));
        assertThat(emitidos.getValue()).hasSize(2)
                .extracting(BoletoEntidad::getNumeroAsiento).containsExactly(3, 4);
        assertThat(emitidos.getValue()).allSatisfy(boleto -> {
            assertThat(boleto.getVentaId()).isEqualTo(venta.getId());
            assertThat(boleto.getViajeId()).isEqualTo(viaje.getViajeId());
            assertThat(boleto.getPrecio()).isEqualByComparingTo("75.00");
            assertThat(boleto.getEstado()).isEqualTo(EstadoBoleto.ACTIVO);
        });
        verify(ventas, never()).findById(any());
        verify(asientos, never()).liberarDeVenta(any());
        verify(eventos, never()).reembolsoVenta(any(), any(), any());
        verifyNoInteractions(ordenes);
    }

    @Test
    void losBoletosSeEmitenConElPrecioDeLaVentaAunqueElViajeHayaCambiadoDePrecio() {
        VentaEntidad venta = ventaPendiente(3);
        // Trips subió el precio del viaje después de la venta
        ReflectionTestUtils.setField(viaje, "precioBoleto", new BigDecimal("90.00"));
        bloqueable(venta);
        when(viajes.findById(viaje.getViajeId())).thenReturn(Optional.of(viaje));
        when(asientos.reservadosDeVenta(venta.getId())).thenReturn(List.of(7, 8, 9));

        servicio.carteraDebitada(ReferenciaCartera.VENTA, venta.getId());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<BoletoEntidad>> guardados = ArgumentCaptor.forClass(List.class);
        verify(boletos).saveAll(guardados.capture());
        assertThat(venta.precioPorBoleto()).isEqualByComparingTo("75.00");
        assertThat(guardados.getValue()).extracting(BoletoEntidad::getPrecio)
                .allSatisfy(precio -> assertThat(precio).isEqualByComparingTo(venta.precioPorBoleto()));
        verify(eventos).boletoVendido(eq(venta), any(), eq(viaje));
    }

    @Test
    void carteraDebitadaConAsientosPerdidosRechazaLaVentaYPideElReembolso() {
        VentaEntidad venta = ventaPendiente(2);
        bloqueable(venta);
        when(viajes.findById(viaje.getViajeId())).thenReturn(Optional.of(viaje));
        // otro cliente tomó un asiento después de que venció la reserva
        when(asientos.reservadosDeVenta(venta.getId())).thenReturn(List.of(3));

        servicio.carteraDebitada(ReferenciaCartera.VENTA, venta.getId());

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.RECHAZADA);
        assertThat(venta.getMotivoRechazo()).isEqualTo(MotivoSaga.TIEMPO_AGOTADO.name());
        InOrder orden = inOrder(ventas, asientos, eventos);
        orden.verify(ventas).saveAndFlush(venta);
        orden.verify(asientos).liberarDeVenta(venta.getId());
        orden.verify(eventos).reembolsoVenta(venta, MotivoSaga.DEBITO_TARDIO, fechaReembolso());
        verify(asientos, never()).confirmarReservas(any());
        verify(eventos, never()).boletoVendido(any(), any(), any());
        verifyNoInteractions(boletos, ordenes);
    }

    @Test
    void carteraDebitadaConViajeEnRutaRechazaLaVentaYPideElReembolso() {
        VistaViajeEntidad enRuta = DatosPrueba.vistaViaje(UUID.randomUUID(), XELA, 40, EstadoViaje.EN_RUTA);
        VentaEntidad venta = DatosPrueba.ventaEnLinea(enRuta, clienteId, 2);
        bloqueable(venta);
        when(viajes.findById(enRuta.getViajeId())).thenReturn(Optional.of(enRuta));
        when(asientos.reservadosDeVenta(venta.getId())).thenReturn(List.of(3, 4));

        servicio.carteraDebitada(ReferenciaCartera.VENTA, venta.getId());

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.RECHAZADA);
        assertThat(venta.getMotivoRechazo()).isEqualTo(MotivoSaga.VIAJE_NO_DISPONIBLE.name());
        verify(ventas).saveAndFlush(venta);
        verify(asientos).liberarDeVenta(venta.getId());
        verify(eventos).reembolsoVenta(venta, MotivoSaga.DEBITO_TARDIO, fechaReembolso());
        verify(asientos, never()).confirmarReservas(any());
        verify(eventos, never()).boletoVendido(any(), any(), any());
        verifyNoInteractions(boletos);
    }

    @Test
    void carteraDebitadaConViajeInexistenteRechazaLaVentaYPideElReembolso() {
        VentaEntidad venta = ventaPendiente(2);
        bloqueable(venta);
        when(viajes.findById(viaje.getViajeId())).thenReturn(Optional.empty());
        when(asientos.reservadosDeVenta(venta.getId())).thenReturn(List.of(3, 4));

        servicio.carteraDebitada(ReferenciaCartera.VENTA, venta.getId());

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.RECHAZADA);
        assertThat(venta.getMotivoRechazo()).isEqualTo(MotivoSaga.VIAJE_NO_DISPONIBLE.name());
        verify(ventas).saveAndFlush(venta);
        verify(asientos).liberarDeVenta(venta.getId());
        verify(eventos).reembolsoVenta(venta, MotivoSaga.DEBITO_TARDIO, fechaReembolso());
        verify(asientos, never()).confirmarReservas(any());
        verify(eventos, never()).boletoVendido(any(), any(), any());
        verifyNoInteractions(boletos);
    }

    @Test
    void carteraDebitadaDeVentaRechazadaSoloPideElReembolso() {
        VentaEntidad venta = ventaRechazada();
        bloqueable(venta);

        servicio.carteraDebitada(ReferenciaCartera.VENTA, venta.getId());

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.RECHAZADA);
        verify(eventos).reembolsoVenta(venta, MotivoSaga.DEBITO_TARDIO, fechaReembolso());
        verify(eventos, never()).boletoVendido(any(), any(), any());
        verify(ventas, never()).saveAndFlush(any());
        verifyNoInteractions(asientos, boletos, viajes, ordenes);
    }

    @Test
    void carteraDebitadaRepetidaDeVentaConfirmadaNoHaceNada() {
        VentaEntidad venta = DatosPrueba.ventaEnLineaConfirmada(viaje, clienteId, 2);
        bloqueable(venta);

        servicio.carteraDebitada(ReferenciaCartera.VENTA, venta.getId());

        sinEfectosEnLaVenta(venta, EstadoVenta.CONFIRMADA);
    }

    @Test
    void carteraDebitadaDeVentaCanceladaNoHaceNada() {
        VentaEntidad venta = ventaCancelada();
        bloqueable(venta);

        servicio.carteraDebitada(ReferenciaCartera.VENTA, venta.getId());

        sinEfectosEnLaVenta(venta, EstadoVenta.CANCELADA);
    }

    @Test
    void carteraDebitadaDeVentaReembolsadaNoHaceNada() {
        VentaEntidad venta = ventaCancelada();
        venta.marcarReembolsada(FECHA_PAGO);
        bloqueable(venta);

        servicio.carteraDebitada(ReferenciaCartera.VENTA, venta.getId());

        sinEfectosEnLaVenta(venta, EstadoVenta.REEMBOLSADA);
    }

    // ===================== CarteraDebitada de un ALQUILER =====================

    @Test
    void carteraDebitadaDeOrdenInexistenteNoHaceNada() {
        UUID ordenId = UUID.randomUUID();
        when(ordenes.findById(ordenId)).thenReturn(Optional.empty());

        servicio.carteraDebitada(ReferenciaCartera.ALQUILER, ordenId);

        verify(ordenes, never()).saveAndFlush(any());
        verifyNoInteractions(ventas, asientos, boletos, viajes, eventos);
    }

    @Test
    void carteraDebitadaConCobroEnCursoDejaLaOrdenPagada() {
        OrdenAlquilerEntidad orden = ordenCobrando();
        existe(orden);

        servicio.carteraDebitada(ReferenciaCartera.ALQUILER, orden.getId());

        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenAlquiler.PAGADA);
        assertThat(orden.getMetodoPago()).isEqualTo(MetodoPago.CARTERA);
        assertThat(orden.getFechaPago()).isEqualTo(FECHA_PAGO);
        InOrder enOrden = inOrder(ordenes, eventos);
        enOrden.verify(ordenes).saveAndFlush(orden);
        enOrden.verify(eventos).alquilerPagado(orden);
        verify(eventos, never()).reembolsoOrden(any(), any(), any());
        verifyNoInteractions(ventas, asientos, boletos, viajes);
    }

    @Test
    void carteraDebitadaRepetidaDeOrdenPagadaConCarteraNoHaceNada() {
        OrdenAlquilerEntidad orden = ordenCobrando();
        orden.confirmarPagoConCartera();
        existe(orden);

        servicio.carteraDebitada(ReferenciaCartera.ALQUILER, orden.getId());

        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenAlquiler.PAGADA);
        verify(ordenes, never()).saveAndFlush(any());
        verifyNoInteractions(eventos, ventas);
    }

    @Test
    void carteraDebitadaDeOrdenPagadaEnEfectivoPideElReembolsoSinCambiarLaOrden() {
        OrdenAlquilerEntidad orden = ordenCobrando();
        // mientras Identity cobraba, el cajero la cobró en efectivo
        orden.pagarEnEfectivo(FECHA_PAGO);
        existe(orden);

        servicio.carteraDebitada(ReferenciaCartera.ALQUILER, orden.getId());

        verify(eventos).reembolsoOrden(orden, MotivoSaga.DEBITO_TARDIO, fechaReembolso());
        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenAlquiler.PAGADA);
        assertThat(orden.getMetodoPago()).isEqualTo(MetodoPago.EFECTIVO);
        verify(ordenes, never()).saveAndFlush(any());
        verify(eventos, never()).alquilerPagado(any());
        verifyNoInteractions(ventas);
    }

    // ===================== DebitoCarteraRechazado =====================

    @Test
    void debitoRechazadoRechazaLaVentaPendienteYLiberaLosAsientos() {
        VentaEntidad venta = ventaPendiente(2);
        bloqueable(venta);

        servicio.debitoRechazado(ReferenciaCartera.VENTA, venta.getId(), "SALDO_INSUFICIENTE");

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.RECHAZADA);
        assertThat(venta.getMotivoRechazo()).isEqualTo("SALDO_INSUFICIENTE");
        InOrder orden = inOrder(ventas, asientos);
        orden.verify(ventas).saveAndFlush(venta);
        orden.verify(asientos).liberarDeVenta(venta.getId());
        verify(ventas, never()).findById(any());
        verifyNoInteractions(eventos, boletos, viajes, ordenes);
    }

    @Test
    void debitoRechazadoSinMotivoUsaRechazado() {
        VentaEntidad venta = ventaPendiente(1);
        bloqueable(venta);

        servicio.debitoRechazado(ReferenciaCartera.VENTA, venta.getId(), null);

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.RECHAZADA);
        assertThat(venta.getMotivoRechazo()).isEqualTo("RECHAZADO");
        verify(asientos).liberarDeVenta(venta.getId());
    }

    @Test
    void debitoRechazadoConMotivoEnBlancoUsaRechazado() {
        VentaEntidad venta = ventaPendiente(1);
        bloqueable(venta);

        servicio.debitoRechazado(ReferenciaCartera.VENTA, venta.getId(), "   ");

        assertThat(venta.getMotivoRechazo()).isEqualTo("RECHAZADO");
        verify(ventas).saveAndFlush(venta);
    }

    @Test
    void debitoRechazadoDeVentaQueYaNoEstaPendienteNoHaceNada() {
        VentaEntidad venta = DatosPrueba.ventaEnLineaConfirmada(viaje, clienteId, 2);
        bloqueable(venta);

        servicio.debitoRechazado(ReferenciaCartera.VENTA, venta.getId(), "SALDO_INSUFICIENTE");

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.CONFIRMADA);
        assertThat(venta.getMotivoRechazo()).isNull();
        verify(ventas, never()).saveAndFlush(any());
        verifyNoInteractions(asientos, eventos);
    }

    @Test
    void debitoRechazadoDeVentaInexistenteNoHaceNada() {
        UUID ventaId = UUID.randomUUID();
        when(ventas.buscarParaActualizar(ventaId)).thenReturn(Optional.empty());

        servicio.debitoRechazado(ReferenciaCartera.VENTA, ventaId, "SALDO_INSUFICIENTE");

        verify(ventas).buscarParaActualizar(ventaId);
        verify(ventas, never()).saveAndFlush(any());
        verifyNoInteractions(asientos, eventos, ordenes);
    }

    @Test
    void debitoRechazadoConCobroEnCursoRechazaLaOrden() {
        OrdenAlquilerEntidad orden = ordenCobrando();
        existe(orden);

        servicio.debitoRechazado(ReferenciaCartera.ALQUILER, orden.getId(), "SALDO_INSUFICIENTE");

        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenAlquiler.RECHAZADA);
        assertThat(orden.getMotivoRechazo()).isEqualTo("SALDO_INSUFICIENTE");
        verify(ordenes).saveAndFlush(orden);
        verifyNoInteractions(eventos, ventas, asientos);
    }

    @Test
    void debitoRechazadoDeOrdenSinMotivoUsaRechazado() {
        OrdenAlquilerEntidad orden = ordenCobrando();
        existe(orden);

        servicio.debitoRechazado(ReferenciaCartera.ALQUILER, orden.getId(), "");

        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenAlquiler.RECHAZADA);
        assertThat(orden.getMotivoRechazo()).isEqualTo("RECHAZADO");
    }

    @Test
    void debitoRechazadoDeOrdenSinCobroEnCursoNoHaceNada() {
        // recién creada: PENDIENTE_PAGO sin método de pago
        OrdenAlquilerEntidad orden = DatosPrueba.orden(clienteId);
        existe(orden);

        servicio.debitoRechazado(ReferenciaCartera.ALQUILER, orden.getId(), "SALDO_INSUFICIENTE");

        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenAlquiler.PENDIENTE_PAGO);
        assertThat(orden.getMotivoRechazo()).isNull();
        verify(ordenes, never()).saveAndFlush(any());
        verifyNoInteractions(eventos);
    }

    @Test
    void debitoRechazadoDeOrdenInexistenteNoHaceNada() {
        UUID ordenId = UUID.randomUUID();
        when(ordenes.findById(ordenId)).thenReturn(Optional.empty());

        servicio.debitoRechazado(ReferenciaCartera.ALQUILER, ordenId, "SALDO_INSUFICIENTE");

        verify(ordenes, never()).saveAndFlush(any());
        verifyNoInteractions(eventos, ventas);
    }

    // ===================== CarteraReembolsada =====================

    @Test
    void carteraReembolsadaDejaReembolsadaLaVentaCanceladaYAvisaAlCliente() {
        VentaEntidad venta = ventaCancelada();
        when(ventas.findById(venta.getId())).thenReturn(Optional.of(venta));

        servicio.carteraReembolsada(ReferenciaCartera.VENTA, venta.getId());

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.REEMBOLSADA);
        verify(ventas).saveAndFlush(venta);
        verify(eventos).ventaReembolsada(eq(venta), ArgumentMatchers.<BoletoEntidad>anyList(), any());
        verifyNoInteractions(asientos, ordenes);
    }

    @Test
    void carteraReembolsadaDejaReembolsadaLaVentaRechazadaYAvisaAlCliente() {
        VentaEntidad venta = ventaRechazada();
        when(ventas.findById(venta.getId())).thenReturn(Optional.of(venta));

        servicio.carteraReembolsada(ReferenciaCartera.VENTA, venta.getId());

        assertThat(venta.getEstado()).isEqualTo(EstadoVenta.REEMBOLSADA);
        verify(ventas).saveAndFlush(venta);
        verify(eventos).ventaReembolsada(eq(venta), ArgumentMatchers.<BoletoEntidad>anyList(), any());
    }

    /** Un reembolso de una venta que ya no está: no se avisa nada y no se toca ninguna tabla. */
    @Test
    void laVentaQueNoExisteNoAvisaNada() {
        UUID ventaId = UUID.randomUUID();
        when(ventas.findById(ventaId)).thenReturn(Optional.empty());

        servicio.carteraReembolsada(ReferenciaCartera.VENTA, ventaId);

        verify(ventas, never()).saveAndFlush(any());
        verifyNoInteractions(eventos, asientos, boletos, ordenes);
    }
}
