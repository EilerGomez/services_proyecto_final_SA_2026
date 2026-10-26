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

import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.VerificarError;
import gt.cunoc.proyecto2.sales.dtos.alquileres.FiltroOrdenes;
import gt.cunoc.proyecto2.sales.dtos.alquileres.OrdenAlquilerRespuesta;
import gt.cunoc.proyecto2.sales.dtos.alquileres.PagoOrdenSolicitud;
import gt.cunoc.proyecto2.sales.errores.alquileres.ErrorOrdenAlquiler;
import gt.cunoc.proyecto2.sales.eventos.PublicadorEventosSales;
import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.CotizacionAlquilerConfirmada;
import gt.cunoc.proyecto2.sales.modelos.alquileres.EstadoOrdenAlquiler;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.MetodoPago;
import gt.cunoc.proyecto2.sales.repositorios.alquileres.OrdenAlquilerRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.clientes.VistaClienteRepositorio;
import java.math.BigDecimal;
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
    private final PublicadorEventosSales eventos =
            mock(PublicadorEventosSales.class);

    private final UUID clienteId = UUID.randomUUID();
    private final PagoOrdenSolicitud pago = new PagoOrdenSolicitud(FECHA_PAGO);

    private OrdenAlquilerServicioImpl servicio;

    @BeforeEach
    void preparar() {
        servicio = new OrdenAlquilerServicioImpl(repositorio, clientes, eventos);
        when(repositorio.saveAndFlush(any(OrdenAlquilerEntidad.class)))
                .thenAnswer(i -> i.getArgument(0));
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
    void miasDevuelveLasOrdenesDelCliente() {
        OrdenAlquilerEntidad primera = DatosPrueba.orden(clienteId);
        OrdenAlquilerEntidad segunda = DatosPrueba.orden(clienteId);

        when(repositorio.findByClienteUsuarioIdOrderByFechaCreacionDesc(clienteId))
                .thenReturn(List.of(primera, segunda));

        List<OrdenAlquilerRespuesta> respuesta =
                servicio.mias(DatosPrueba.cliente(clienteId));

        assertThat(respuesta)
                .extracting(OrdenAlquilerRespuesta::ordenId)
                .containsExactly(primera.getId(), segunda.getId());
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
}