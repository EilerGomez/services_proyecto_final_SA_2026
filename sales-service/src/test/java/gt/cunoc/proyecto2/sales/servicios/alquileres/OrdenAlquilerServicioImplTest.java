package gt.cunoc.proyecto2.sales.servicios.alquileres;

import static gt.cunoc.proyecto2.sales.DatosPrueba.FECHA_PAGO;
import static gt.cunoc.proyecto2.sales.DatosPrueba.GUATE;
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

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.VerificarError;
import gt.cunoc.proyecto2.sales.dtos.alquileres.FiltroOrdenes;
import gt.cunoc.proyecto2.sales.dtos.alquileres.OrdenAlquilerRespuesta;
import gt.cunoc.proyecto2.sales.dtos.alquileres.PagoOrdenSolicitud;
import gt.cunoc.proyecto2.sales.dtos.alquileres.ReembolsoOrdenSolicitud;
import gt.cunoc.proyecto2.sales.dtos.tarifas.ReglaReembolsoRespuesta;
import gt.cunoc.proyecto2.sales.errores.alquileres.ErrorOrdenAlquiler;
import gt.cunoc.proyecto2.sales.eventos.MotivoSaga;
import gt.cunoc.proyecto2.sales.eventos.PublicadorEventosSales;
import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.AlquilerCancelado;
import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.CotizacionAlquilerConfirmada;
import gt.cunoc.proyecto2.sales.modelos.alquileres.EstadoOrdenAlquiler;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import gt.cunoc.proyecto2.sales.modelos.tarifas.EstadoAlquilerTarifa;
import gt.cunoc.proyecto2.sales.modelos.ventas.MetodoPago;
import gt.cunoc.proyecto2.sales.repositorios.alquileres.OrdenAlquilerRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.clientes.VistaClienteRepositorio;
import gt.cunoc.proyecto2.sales.servicios.tarifas.TarifaReembolsoServicio;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

class OrdenAlquilerServicioImplTest {

    private final OrdenAlquilerRepositorio repositorio =
            mock(OrdenAlquilerRepositorio.class);
    private final VistaClienteRepositorio clientes =
            mock(VistaClienteRepositorio.class);
    private final TarifaReembolsoServicio tarifas =
            mock(TarifaReembolsoServicio.class);
    private final PublicadorEventosSales eventos =
            mock(PublicadorEventosSales.class);

    private final UUID clienteId = UUID.randomUUID();
    private final PagoOrdenSolicitud pago = new PagoOrdenSolicitud(FECHA_PAGO);

    private OrdenAlquilerServicioImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new OrdenAlquilerServicioImpl(repositorio, clientes, tarifas, eventos);
        when(repositorio.saveAndFlush(any(OrdenAlquilerEntidad.class)))
                .thenAnswer(i -> i.getArgument(0));
    }

    /** La tarifa que resolvería Sales para una cancelación. */
    private void laTarifaDevuelve(String porcentaje) {
        when(tarifas.aplicable(any(EstadoAlquilerTarifa.class), ArgumentMatchers.anyLong()))
                .thenReturn(new ReglaReembolsoRespuesta(UUID.randomUUID(), true,
                        new BigDecimal(porcentaje), "Dentro del plazo", 24));
    }

    private static AlquilerCancelado cancelacion(UUID alquilerId, String estadoAnterior, long horas) {
        return new AlquilerCancelado(alquilerId, XELA, estadoAnterior,
                OffsetDateTime.parse("2026-11-05T07:00:00-06:00"),
                OffsetDateTime.parse("2026-11-03T07:00:00-06:00"), horas, "Cambio de planes",
                UUID.randomUUID(), "Ana López", "ana@correo.com");
    }

    private OrdenAlquilerEntidad existe() {
        OrdenAlquilerEntidad orden = DatosPrueba.orden(clienteId);
        when(repositorio.findById(orden.getId()))
                .thenReturn(Optional.of(orden));
        return orden;
    }

    @Test
    void listarDevuelveLasOrdenesRecientesPrimero() {
        OrdenAlquilerEntidad orden = DatosPrueba.orden(clienteId);
        Sort ordenamiento = Sort.by(Sort.Order.desc("fechaCreacion"));

        when(repositorio.findAll(
                ArgumentMatchers.<Specification<OrdenAlquilerEntidad>>any(),
                eq(ordenamiento)))
                .thenReturn(List.of(orden));

        List<OrdenAlquilerRespuesta> respuesta =
                servicio.listar(new FiltroOrdenes(XELA, null));

        assertThat(respuesta)
                .containsExactly(new OrdenAlquilerRespuesta(orden));
    }

    @Test
    void miasDevuelveLasOrdenesDelClienteYLasDeSuNit() {
        OrdenAlquilerEntidad primera = DatosPrueba.orden(clienteId);
        OrdenAlquilerEntidad segunda = DatosPrueba.orden(clienteId);
        when(clientes.findById(clienteId)).thenReturn(Optional.of(DatosPrueba.vistaCliente(clienteId)));
        when(repositorio.mias(clienteId, "1234567-8")).thenReturn(List.of(primera, segunda));

        List<OrdenAlquilerRespuesta> respuesta =
                servicio.mias(DatosPrueba.cliente(clienteId));

        assertThat(respuesta)
                .extracting(OrdenAlquilerRespuesta::ordenId)
                .containsExactly(primera.getId(), segunda.getId());
    }

    /**
     * El NIT viaja en mayúsculas porque la consulta compara contra upper(cliente_nit): si se mandara
     * tal cual, una orden guardada con el NIT en minúsculas no aparecería.
     */
    @Test
    void miasBuscaElNitEnMayusculas() {
        when(clientes.findById(clienteId))
                .thenReturn(Optional.of(DatosPrueba.vistaCliente(clienteId, " 1234567-k ", "Ana", null)));
        when(repositorio.mias(clienteId, "1234567-K")).thenReturn(List.of());

        servicio.mias(DatosPrueba.cliente(clienteId));

        verify(repositorio).mias(clienteId, "1234567-K");
    }

    /** Sin perfil no hay NIT que buscar: solo salen las órdenes ya enlazadas a la cuenta. */
    @Test
    void miasSinPerfilBuscaSoloPorCuenta() {
        when(clientes.findById(clienteId)).thenReturn(Optional.empty());
        when(repositorio.mias(clienteId, null)).thenReturn(List.of(DatosPrueba.orden(clienteId)));

        assertThat(servicio.mias(DatosPrueba.cliente(clienteId))).hasSize(1);
        verify(repositorio).mias(clienteId, null);
    }

    /** "CF" no identifica a nadie: no se puede usar para reclamar órdenes. */
    @Test
    void miasConNitDeConsumidorFinalNoBuscaPorNit() {
        when(clientes.findById(clienteId))
                .thenReturn(Optional.of(DatosPrueba.vistaCliente(clienteId, "CF", "Consumidor final", null)));
        when(repositorio.mias(clienteId, null)).thenReturn(List.of());

        servicio.mias(DatosPrueba.cliente(clienteId));

        verify(repositorio).mias(clienteId, null);
    }

    @Test
    void obtenerOrdenInexistenteEsNoExiste() {
        UUID id = UUID.randomUUID();
        when(repositorio.findById(id)).thenReturn(Optional.empty());

        VerificarError.lanza(
                () -> servicio.obtener(id, DatosPrueba.cliente(clienteId)),
                ErrorOrdenAlquiler.ORDEN_NO_EXISTE);
    }

    @Test
    void elClienteDuenoVeSuOrden() {
        OrdenAlquilerEntidad orden = existe();

        OrdenAlquilerRespuesta respuesta =
                servicio.obtener(orden.getId(), DatosPrueba.cliente(clienteId));

        assertThat(respuesta.ordenId()).isEqualTo(orden.getId());
        assertThat(respuesta.alquilerId()).isEqualTo(orden.getAlquilerId());
        assertThat(respuesta.sucursalId()).isEqualTo(XELA);
        assertThat(respuesta.monto()).isEqualByComparingTo("2600.00");
        assertThat(respuesta.estado())
                .isEqualTo(EstadoOrdenAlquiler.PENDIENTE_PAGO);
    }

    @Test
    void otroClienteNoVeLaOrden() {
        OrdenAlquilerEntidad orden = existe();

        VerificarError.lanza(
                () -> servicio.obtener(orden.getId(), DatosPrueba.cliente()),
                ErrorOrdenAlquiler.ORDEN_AJENA);
    }

    @Test
    void elPersonalDeOtraSucursalNoVeLaOrden() {
        OrdenAlquilerEntidad orden = existe();

        VerificarError.lanza(
                () -> servicio.obtener(orden.getId(), DatosPrueba.cajero(GUATE)),
                ErrorOrdenAlquiler.ORDEN_AJENA);

        VerificarError.lanza(
                () -> servicio.obtener(
                        orden.getId(), DatosPrueba.adminSucursal(GUATE)),
                ErrorOrdenAlquiler.ORDEN_AJENA);
    }

    @Test
    void pagarConCarteraPideElDebito() {
        OrdenAlquilerEntidad orden = existe();

        OrdenAlquilerRespuesta respuesta = servicio.pagarConCartera(
                orden.getId(), pago, DatosPrueba.cliente(clienteId));

        assertThat(respuesta.estado())
                .isEqualTo(EstadoOrdenAlquiler.PENDIENTE_PAGO);
        assertThat(respuesta.metodoPago()).isEqualTo(MetodoPago.CARTERA);
        assertThat(respuesta.fechaPago()).isEqualTo(FECHA_PAGO);
        assertThat(orden.cobroConCarteraEnCurso()).isTrue();

        InOrder enOrden = inOrder(repositorio, eventos);
        enOrden.verify(repositorio).saveAndFlush(orden);
        enOrden.verify(eventos).debitoOrden(orden);

        verify(eventos, never()).alquilerPagado(any());
    }

    @Test
    void pagarConCarteraSeReintentaDesdeRechazada() {
        OrdenAlquilerEntidad orden = existe();
        orden.solicitarPagoConCartera(FECHA_PAGO);
        orden.rechazarPagoConCartera("SALDO_INSUFICIENTE");

        OrdenAlquilerRespuesta respuesta = servicio.pagarConCartera(
                orden.getId(), pago, DatosPrueba.cliente(clienteId));

        assertThat(respuesta.estado())
                .isEqualTo(EstadoOrdenAlquiler.PENDIENTE_PAGO);
        assertThat(respuesta.motivoRechazo()).isNull();

        verify(eventos).debitoOrden(orden);
    }

    @Test
    void pagarConCarteraDeOrdenAjenaEsOrdenAjena() {
        OrdenAlquilerEntidad orden = existe();

        VerificarError.lanza(
                () -> servicio.pagarConCartera(
                        orden.getId(), pago, DatosPrueba.cliente()),
                ErrorOrdenAlquiler.ORDEN_AJENA);

        assertThat(orden.getMetodoPago()).isNull();
        verify(repositorio, never()).saveAndFlush(any());
        verifyNoInteractions(eventos);
    }

    @Test
    void pagarConCarteraMientrasSeCobraEsPagoEnProceso() {
        OrdenAlquilerEntidad orden = existe();
        orden.solicitarPagoConCartera(FECHA_PAGO);

        VerificarError.lanza(
                () -> servicio.pagarConCartera(
                        orden.getId(), pago, DatosPrueba.cliente(clienteId)),
                ErrorOrdenAlquiler.PAGO_EN_PROCESO);

        verify(repositorio, never()).saveAndFlush(any());
        verifyNoInteractions(eventos);
    }

    @Test
    void pagarConCarteraOrdenPagadaEsEstadoInvalido() {
        OrdenAlquilerEntidad orden = existe();
        orden.pagarEnEfectivo(FECHA_PAGO);

        VerificarError.lanza(
                () -> servicio.pagarConCartera(
                        orden.getId(), pago, DatosPrueba.cliente(clienteId)),
                ErrorOrdenAlquiler.ESTADO_INVALIDO);

        verify(repositorio, never()).saveAndFlush(any());
        verifyNoInteractions(eventos);
    }

    // ----- registrarOrden (CotizacionAlquilerConfirmada) -----

    private CotizacionAlquilerConfirmada cotizacion(UUID alquilerId, UUID usuarioId, String nit) {
        return new CotizacionAlquilerConfirmada(alquilerId, XELA, usuarioId, nit, "Colegio San José",
                "colegio@correo.com", "Quetzaltenango", "Antigua Guatemala",
                DatosPrueba.RECORRIDO.origenLatitud(), DatosPrueba.RECORRIDO.origenLongitud(),
                DatosPrueba.RECORRIDO.destinoLatitud(), DatosPrueba.RECORRIDO.destinoLongitud(),
                DatosPrueba.SALIDA, DatosPrueba.RETORNO, new BigDecimal("2600.00"));
    }

    @Test
    void laOrdenDeUnAlquilerEnLineaTomaElNitDelPerfil() {
        UUID alquilerId = UUID.randomUUID();
        when(repositorio.existsByAlquilerId(alquilerId)).thenReturn(false);
        when(clientes.findById(clienteId)).thenReturn(Optional.of(DatosPrueba.vistaCliente(clienteId)));

        servicio.registrarOrden(cotizacion(alquilerId, clienteId, null));

        ArgumentCaptor<OrdenAlquilerEntidad> captor = ArgumentCaptor.forClass(OrdenAlquilerEntidad.class);
        verify(repositorio).save(captor.capture());
        assertThat(captor.getValue().getClienteNit()).isEqualTo("1234567-8");
        assertThat(captor.getValue().getClienteUsuarioId()).isEqualTo(clienteId);
        assertThat(captor.getValue().getEstado()).isEqualTo(EstadoOrdenAlquiler.PENDIENTE_PAGO);
    }

    @Test
    void laOrdenDeMostradorConservaElNitEscrito() {
        UUID alquilerId = UUID.randomUUID();
        when(repositorio.existsByAlquilerId(alquilerId)).thenReturn(false);

        servicio.registrarOrden(cotizacion(alquilerId, null, "9876543-2"));

        ArgumentCaptor<OrdenAlquilerEntidad> captor = ArgumentCaptor.forClass(OrdenAlquilerEntidad.class);
        verify(repositorio).save(captor.capture());
        assertThat(captor.getValue().getClienteNit()).isEqualTo("9876543-2");
        verifyNoInteractions(clientes);
    }

    @Test
    void unaCotizacionRepetidaNoCreaOtraOrden() {
        UUID alquilerId = UUID.randomUUID();
        when(repositorio.existsByAlquilerId(alquilerId)).thenReturn(true);

        servicio.registrarOrden(cotizacion(alquilerId, clienteId, null));

        verify(repositorio, never()).save(any());
    }

    // ----- AlquilerCancelado (Trips) -----

    /** Pagada con cartera: se aplica el porcentaje de la tarifa y Wallet devuelve solo esa parte. */
    @Test
    void cancelarUnaOrdenPagadaConCarteraPideElReembolsoParcial() {
        OrdenAlquilerEntidad orden = existe();
        orden.solicitarPagoConCartera(FECHA_PAGO);
        orden.confirmarPagoConCartera();
        when(repositorio.findByAlquilerId(orden.getAlquilerId())).thenReturn(Optional.of(orden));
        laTarifaDevuelve("80.00");

        servicio.registrarCancelacion(cancelacion(orden.getAlquilerId(), "ASIGNADO", 30), FECHA_PAGO);

        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenAlquiler.CANCELADA);
        assertThat(orden.getPorcentajeReembolso()).isEqualByComparingTo("80.00");
        assertThat(orden.getMontoReembolsado()).isEqualByComparingTo("2080.00");   // 80% de 2600
        assertThat(orden.montoAReembolsar()).isEqualByComparingTo("2080.00");
        assertThat(orden.tieneReembolsoPendiente()).isTrue();
        verify(eventos).reembolsoOrden(orden, MotivoSaga.ALQUILER_CANCELADO, FECHA_PAGO);
    }

    /** Pagada en efectivo: no hay evento, el dinero lo entrega la ventanilla. */
    @Test
    void cancelarUnaOrdenPagadaEnEfectivoQuedaPendienteDeVentanilla() {
        OrdenAlquilerEntidad orden = existe();
        orden.pagarEnEfectivo(FECHA_PAGO);
        when(repositorio.findByAlquilerId(orden.getAlquilerId())).thenReturn(Optional.of(orden));
        laTarifaDevuelve("95.00");

        servicio.registrarCancelacion(cancelacion(orden.getAlquilerId(), "PAGADO", 100), FECHA_PAGO);

        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenAlquiler.CANCELADA);
        assertThat(orden.getMontoReembolsado()).isEqualByComparingTo("2470.00");   // 95% de 2600
        assertThat(orden.tieneReembolsoPendiente()).isTrue();
        verifyNoInteractions(eventos);
    }

    /** Sin pago no hay tarifa que resolver ni dinero que devolver: la orden se cierra de una vez. */
    @Test
    void cancelarUnaOrdenSinPagarNoConsultaLaTarifa() {
        OrdenAlquilerEntidad orden = existe();
        when(repositorio.findByAlquilerId(orden.getAlquilerId())).thenReturn(Optional.of(orden));

        servicio.registrarCancelacion(cancelacion(orden.getAlquilerId(), "COTIZADO", 500), FECHA_PAGO);

        assertThat(orden.getEstado()).isEqualTo(EstadoOrdenAlquiler.CANCELADA);
        assertThat(orden.getMontoReembolsado()).isEqualByComparingTo("0.00");
        assertThat(orden.tieneReembolsoPendiente()).isFalse();
        verifyNoInteractions(tarifas);
        verifyNoInteractions(eventos);
    }

    /** Un alquiler cancelado antes de cotizarse nunca tuvo orden de pago. */
    @Test
    void cancelarUnAlquilerSinOrdenNoHaceNada() {
        UUID alquilerId = UUID.randomUUID();
        when(repositorio.findByAlquilerId(alquilerId)).thenReturn(Optional.empty());

        servicio.registrarCancelacion(cancelacion(alquilerId, "SOLICITADO", 500), FECHA_PAGO);

        verify(repositorio, never()).saveAndFlush(any());
        verifyNoInteractions(eventos);
    }

    @Test
    void unAlquilerCanceladoDosVecesNoVuelveAMoverDinero() {
        OrdenAlquilerEntidad orden = existe();
        orden.pagarEnEfectivo(FECHA_PAGO);
        orden.cancelar("Cambio de planes", FECHA_PAGO, 30, new BigDecimal("80.00"), "Dentro del plazo");
        when(repositorio.findByAlquilerId(orden.getAlquilerId())).thenReturn(Optional.of(orden));

        servicio.registrarCancelacion(cancelacion(orden.getAlquilerId(), "ASIGNADO", 30), FECHA_PAGO);

        verify(repositorio, never()).saveAndFlush(any());
        verifyNoInteractions(tarifas);
        verifyNoInteractions(eventos);
    }

    // ----- Reembolso en ventanilla -----

    @Test
    void laVentanillaEntregaElEfectivoYLaOrdenQuedaReembolsada() {
        OrdenAlquilerEntidad orden = existe();
        orden.pagarEnEfectivo(FECHA_PAGO);
        orden.cancelar("Cambio de planes", FECHA_PAGO, 30, new BigDecimal("80.00"), "Dentro del plazo");
        UsuarioActual cajero = DatosPrueba.cajero(XELA);

        OrdenAlquilerRespuesta respuesta = servicio.reembolsar(orden.getId(),
                new ReembolsoOrdenSolicitud(0), cajero);

        assertThat(respuesta.estado()).isEqualTo(EstadoOrdenAlquiler.REEMBOLSADA);
        assertThat(orden.getReembolsadoPor()).isEqualTo(cajero.usuarioId());
        assertThat(orden.getFechaReembolso()).isNotNull();
    }

    /** Si se pagó con cartera la ventanilla no entrega nada: solo se vuelve a pedir el reembolso. */
    @Test
    void laVentanillaNoEntregaElDineroDeUnaOrdenPagadaConCartera() {
        OrdenAlquilerEntidad orden = existe();
        orden.solicitarPagoConCartera(FECHA_PAGO);
        orden.confirmarPagoConCartera();
        orden.cancelar("Cambio de planes", FECHA_PAGO, 30, new BigDecimal("80.00"), "Dentro del plazo");

        OrdenAlquilerRespuesta respuesta = servicio.reembolsar(orden.getId(),
                new ReembolsoOrdenSolicitud(0), DatosPrueba.cajero(XELA));

        assertThat(respuesta.estado()).isEqualTo(EstadoOrdenAlquiler.CANCELADA);
        verify(eventos).reembolsoOrden(eq(orden), eq(MotivoSaga.ALQUILER_CANCELADO),
                ArgumentMatchers.any());
    }

    @Test
    void noSePuedeReembolsarUnaOrdenQueNoEstaCancelada() {
        OrdenAlquilerEntidad orden = existe();
        orden.pagarEnEfectivo(FECHA_PAGO);

        VerificarError.lanza(() -> servicio.reembolsar(orden.getId(), new ReembolsoOrdenSolicitud(0),
                DatosPrueba.cajero(XELA)), ErrorOrdenAlquiler.ESTADO_INVALIDO_REEMBOLSO);
    }

    @Test
    void noSePuedeReembolsarDosVecesLaMismaOrden() {
        OrdenAlquilerEntidad orden = existe();
        orden.pagarEnEfectivo(FECHA_PAGO);
        orden.cancelar("Cambio de planes", FECHA_PAGO, 30, new BigDecimal("80.00"), "Dentro del plazo");
        orden.marcarReembolsada(FECHA_PAGO, UUID.randomUUID());

        VerificarError.lanza(() -> servicio.reembolsar(orden.getId(), new ReembolsoOrdenSolicitud(0),
                DatosPrueba.cajero(XELA)), ErrorOrdenAlquiler.ORDEN_YA_REEMBOLSADA);
    }
}