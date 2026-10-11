package gt.cunoc.proyecto2.sales.servicios.ventas;

import static gt.cunoc.proyecto2.sales.DatosPrueba.FECHA_PAGO;
import static gt.cunoc.proyecto2.sales.DatosPrueba.GUATE;
import static gt.cunoc.proyecto2.sales.DatosPrueba.SALIDA;
import static gt.cunoc.proyecto2.sales.DatosPrueba.XELA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.DatosPrueba;
import gt.cunoc.proyecto2.sales.VerificarError;
import gt.cunoc.proyecto2.sales.clientes.trips.ClienteTrips;
import gt.cunoc.proyecto2.sales.dtos.trips.VendibleRespuesta;
import gt.cunoc.proyecto2.sales.dtos.ventas.BoletoRespuesta;
import gt.cunoc.proyecto2.sales.dtos.ventas.CancelarVentaSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.FiltroVentas;
import gt.cunoc.proyecto2.sales.dtos.ventas.NuevaVentaEnLineaSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.NuevaVentaMostradorSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.VentaPendienteRespuesta;
import gt.cunoc.proyecto2.sales.dtos.ventas.VentaRespuesta;
import gt.cunoc.proyecto2.sales.errores.ventas.ErrorVenta;
import gt.cunoc.proyecto2.sales.errores.viajes.ErrorViaje;
import gt.cunoc.proyecto2.sales.eventos.MotivoSaga;
import gt.cunoc.proyecto2.sales.eventos.PublicadorEventosSales;
import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.CanalVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.EstadoBoleto;
import gt.cunoc.proyecto2.sales.modelos.ventas.EstadoVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.MetodoPago;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import gt.cunoc.proyecto2.sales.repositorios.asientos.AsientoViajeRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.clientes.VistaClienteRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.ventas.BoletoRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.ventas.VentaRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.viajes.VistaViajeRepositorio;
import gt.cunoc.proyecto2.sales.servicios.viajes.VistaViajeServicio;
import gt.cunoc.proyecto2.sales.utilidades.Fechas;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

class VentaServicioImplTest {

    private static final int MINUTOS_RESERVA = 5;
    private static final List<EstadoVenta> AFECTADAS
            = List.of(EstadoVenta.PENDIENTE_PAGO, EstadoVenta.CONFIRMADA);

    private final VentaRepositorio repositorio = mock(VentaRepositorio.class);
    private final BoletoRepositorio boletos = mock(BoletoRepositorio.class);
    private final AsientoViajeRepositorio asientos
            = mock(AsientoViajeRepositorio.class);
    private final VistaViajeServicio viajes = mock(VistaViajeServicio.class);
    private final VistaViajeRepositorio viajeRepositorio
            = mock(VistaViajeRepositorio.class);
    private final VistaClienteRepositorio clientes
            = mock(VistaClienteRepositorio.class);
    private final ClienteTrips trips = mock(ClienteTrips.class);
    private final PublicadorEventosSales eventos
            = mock(PublicadorEventosSales.class);

    private final VentaServicioImpl servicio = new VentaServicioImpl(
            repositorio, boletos, asientos, viajes, viajeRepositorio,
            clientes, trips, eventos, MINUTOS_RESERVA);

    private final VistaViajeEntidad viaje = DatosPrueba.vistaViaje();
    private final UsuarioActual cajeroXela = DatosPrueba.cajero(XELA);
    private final UsuarioActual adminXela = DatosPrueba.adminSucursal(XELA);

    private void tienePerfil(UsuarioActual cliente) {
        when(clientes.findById(cliente.usuarioId())).thenReturn(
                Optional.of(DatosPrueba.vistaCliente(cliente.usuarioId())));
    }

    private void viajeALaVenta() {
        when(viajes.obtenerALaVenta(viaje.getViajeId())).thenReturn(viaje);
    }

    private void tripsDiceVendible(boolean vendible, String motivo) {
        when(trips.consultarVendible(viaje.getViajeId())).thenReturn(
                new VendibleRespuesta(
                        viaje.getViajeId(), vendible, motivo,
                        "PROGRAMADO", "REGULAR", SALIDA,
                        DatosPrueba.PRECIO_BOLETO, 40, XELA));
    }

    private void guardarDevuelveElMismo() {
        when(repositorio.saveAndFlush(any(VentaEntidad.class)))
                .thenAnswer(i -> i.getArgument(0));
        when(boletos.saveAll(ArgumentMatchers.<BoletoEntidad>anyList()))
                .thenAnswer(i -> i.getArgument(0));
    }

    private void laVentaExiste(VentaEntidad venta) {
        when(repositorio.findById(venta.getId()))
                .thenReturn(Optional.of(venta));
    }

    private void elViajeEstaEnLaVista(VistaViajeEntidad enVista) {
        when(viajeRepositorio.findById(viaje.getViajeId()))
                .thenReturn(Optional.ofNullable(enVista));
    }

    private List<BoletoEntidad> boletosDe(
            VentaEntidad venta, Integer... numeros) {
        List<BoletoEntidad> suyos = DatosPrueba.boletos(venta, numeros);
        when(boletos.findByVentaIdOrderByNumeroAsientoAsc(venta.getId()))
                .thenReturn(suyos);
        return suyos;
    }

    private VentaEntidad ventaGuardada() {
        ArgumentCaptor<VentaEntidad> guardada
                = ArgumentCaptor.forClass(VentaEntidad.class);
        verify(repositorio).saveAndFlush(guardada.capture());
        return guardada.getValue();
    }

    private NuevaVentaMostradorSolicitud mostradorConsumidorFinal(
            List<Integer> asientosPedidos) {
        return new NuevaVentaMostradorSolicitud(
                viaje.getViajeId(), asientosPedidos,
                null, null, null, FECHA_PAGO);
    }

    private NuevaVentaEnLineaSolicitud enLinea(
            List<Integer> asientosPedidos) {
        return new NuevaVentaEnLineaSolicitud(
                viaje.getViajeId(), asientosPedidos, FECHA_PAGO);
    }

    private static CancelarVentaSolicitud cancelacion(Integer version) {
        return new CancelarVentaSolicitud(
                "El cliente ya no viaja", FECHA_PAGO.plusDays(1), version);
    }

    @Test
    void mostradorDeUnViajeQueSaleDeOtraSucursal() {
        viajeALaVenta();

        VerificarError.lanza(
                () -> servicio.venderEnMostrador(
                        mostradorConsumidorFinal(List.of(1)),
                        DatosPrueba.cajero(GUATE)),
                ErrorVenta.OTRA_SUCURSAL);

        verifyNoInteractions(trips, repositorio, asientos, eventos);
    }

    @Test
    void mostradorConAsientosRepetidos() {
        viajeALaVenta();

        VerificarError.lanza(
                () -> servicio.venderEnMostrador(
                        mostradorConsumidorFinal(List.of(4, 4)), cajeroXela),
                ErrorVenta.ASIENTOS_REPETIDOS);

        verifyNoInteractions(trips, repositorio, asientos, eventos);
    }

    @Test
    void mostradorConAsientoQueNoExisteEnElBus() {
        viajeALaVenta();

        VerificarError.lanza(
                () -> servicio.venderEnMostrador(
                        mostradorConsumidorFinal(List.of(1, 41)), cajeroXela),
                ErrorVenta.ASIENTO_FUERA_DE_RANGO);

        verifyNoInteractions(trips, repositorio, asientos, eventos);
    }

    @Test
    void mostradorCuandoTripsDiceQueYaNoSeVende() {
        viajeALaVenta();
        tripsDiceVendible(false, "VIAJE_YA_INICIO");

        VerificarError.lanza(
                () -> servicio.venderEnMostrador(
                        mostradorConsumidorFinal(List.of(1)), cajeroXela),
                ErrorViaje.VIAJE_NO_VENDIBLE);

        assertThatThrownBy(() -> servicio.venderEnMostrador(
                mostradorConsumidorFinal(List.of(1)), cajeroXela))
                .hasMessageContaining("VIAJE_YA_INICIO");

        verifyNoInteractions(repositorio, asientos, boletos, eventos);
    }

    @Test
    void mostradorConAsientoQueOtroYaTomo() {
        viajeALaVenta();
        tripsDiceVendible(true, null);
        guardarDevuelveElMismo();

        when(asientos.vender(
                eq(viaje.getViajeId()), eq(List.of(1, 3)), any(UUID.class)))
                .thenReturn(1);

        VerificarError.lanza(
                () -> servicio.venderEnMostrador(
                        mostradorConsumidorFinal(List.of(3, 1)), cajeroXela),
                ErrorVenta.ASIENTO_OCUPADO);

        verifyNoInteractions(boletos, eventos);
    }

    @Test
    void mostradorEmiteLosBoletosYPublicaBoletoVendido() {
        viajeALaVenta();
        tripsDiceVendible(true, null);
        guardarDevuelveElMismo();

        when(asientos.vender(
                eq(viaje.getViajeId()), eq(List.of(1, 3)), any(UUID.class)))
                .thenReturn(2);

        VentaRespuesta respuesta = servicio.venderEnMostrador(
                mostradorConsumidorFinal(List.of(3, 1)), cajeroXela);

        VentaEntidad venta = ventaGuardada();

        assertThat(venta.getCajeroUsuarioId()).isEqualTo(cajeroXela.usuarioId());
        assertThat(venta.getCantidadBoletos()).isEqualTo(2);
        assertThat(respuesta.ventaId()).isEqualTo(venta.getId());
        assertThat(respuesta.viajeId()).isEqualTo(viaje.getViajeId());
        assertThat(respuesta.sucursalId()).isEqualTo(XELA);
        assertThat(respuesta.estado()).isEqualTo(EstadoVenta.CONFIRMADA);
        assertThat(respuesta.canal()).isEqualTo(CanalVenta.PRESENCIAL);
        assertThat(respuesta.metodoPago()).isEqualTo(MetodoPago.EFECTIVO);
        assertThat(respuesta.montoTotal()).isEqualByComparingTo("150.00");
        assertThat(respuesta.fechaPago()).isEqualTo(FECHA_PAGO);

        assertThat(respuesta.boletos())
                .extracting(BoletoRespuesta::numeroAsiento)
                .containsExactly(1, 3);
        assertThat(respuesta.boletos())
                .extracting(BoletoRespuesta::estado)
                .containsOnly(EstadoBoleto.ACTIVO);
        assertThat(respuesta.boletos())
                .allSatisfy(b -> assertThat(b.precio())
                        .isEqualByComparingTo("75.00"));

        assertThat(respuesta.viaje()).isNotNull();
        assertThat(respuesta.viaje().origenNombre()).isEqualTo("Quetzaltenango");
        assertThat(respuesta.viaje().destinoNombre()).isEqualTo("Guatemala");
        assertThat(respuesta.viaje().busPlaca()).isEqualTo("P-123ABC");
        assertThat(respuesta.viaje().fechaSalida()).isAtSameInstantAs(SALIDA);

        verify(asientos).vender(
                viaje.getViajeId(), List.of(1, 3), venta.getId());

        verify(eventos).boletoVendido(
                same(venta),
                argThat(emitidos -> emitidos.size() == 2
                        && emitidos.stream().allMatch(
                                b -> venta.getId().equals(b.getVentaId())
                                && viaje.getViajeId()
                                        .equals(b.getViajeId()))),
                same(viaje));

        InOrder orden = inOrder(repositorio, asientos, boletos, eventos);
        orden.verify(repositorio).saveAndFlush(any(VentaEntidad.class));
        orden.verify(asientos).vender(any(), any(), any());
        orden.verify(boletos)
                .saveAll(ArgumentMatchers.<BoletoEntidad>anyList());
        orden.verify(eventos).boletoVendido(any(), any(), any());
    }

    @Test
    void enLineaSinPerfilDeCliente() {
        UsuarioActual cliente = DatosPrueba.cliente();
        when(clientes.findById(cliente.usuarioId()))
                .thenReturn(Optional.empty());

        VerificarError.lanza(
                () -> servicio.venderEnLinea(enLinea(List.of(1)), cliente),
                ErrorVenta.CLIENTE_SIN_PERFIL);

        verifyNoInteractions(viajes, trips, repositorio, asientos, eventos);
    }

    @Test
    void enLineaConAsientoQueOtroYaTomoNoPideElDebito() {
        UsuarioActual cliente = DatosPrueba.cliente();
        tienePerfil(cliente);
        viajeALaVenta();
        tripsDiceVendible(true, null);
        guardarDevuelveElMismo();

        when(asientos.reservar(
                eq(viaje.getViajeId()), eq(List.of(1, 3)),
                any(UUID.class), any(OffsetDateTime.class)))
                .thenReturn(1);

        VerificarError.lanza(
                () -> servicio.venderEnLinea(enLinea(List.of(3, 1)), cliente),
                ErrorVenta.ASIENTO_OCUPADO);

        verify(eventos, never()).debitoVenta(any());
    }

    @Test
    void enLineaReservaLosAsientosYPideElDebito() {
        UsuarioActual cliente = DatosPrueba.cliente();
        tienePerfil(cliente);
        viajeALaVenta();
        tripsDiceVendible(true, null);
        guardarDevuelveElMismo();

        when(asientos.reservar(
                eq(viaje.getViajeId()), eq(List.of(1, 3)),
                any(UUID.class), any(OffsetDateTime.class)))
                .thenReturn(2);

        Instant antes = Instant.now();
        VentaPendienteRespuesta respuesta
                = servicio.venderEnLinea(enLinea(List.of(3, 1)), cliente);
        Instant despues = Instant.now();

        VentaEntidad venta = ventaGuardada();

        assertThat(venta.getCanal()).isEqualTo(CanalVenta.EN_LINEA);
        assertThat(venta.getMetodoPago()).isEqualTo(MetodoPago.CARTERA);
        assertThat(venta.getClienteUsuarioId()).isEqualTo(cliente.usuarioId());
        assertThat(venta.getClienteNit()).isEqualTo("1234567-8");
        assertThat(venta.getClienteNombre()).isEqualTo("Ana L\u00f3pez");
        assertThat(venta.getClienteCorreo()).isEqualTo("ana@correo.com");
        assertThat(venta.getFechaPago()).isEqualTo(FECHA_PAGO);

        assertThat(respuesta.ventaId()).isEqualTo(venta.getId());
        assertThat(respuesta.estado()).isEqualTo(EstadoVenta.PENDIENTE_PAGO);
        assertThat(respuesta.montoTotal()).isEqualByComparingTo("150.00");
        assertThat(respuesta.asientos()).containsExactly(1, 3);
        assertThat(respuesta.reservadoHasta().toInstant()).isBetween(
                antes.plusSeconds(MINUTOS_RESERVA * 60L),
                despues.plusSeconds(MINUTOS_RESERVA * 60L));

        ArgumentCaptor<OffsetDateTime> hasta
                = ArgumentCaptor.forClass(OffsetDateTime.class);

        verify(asientos).reservar(
                eq(viaje.getViajeId()), eq(List.of(1, 3)),
                eq(venta.getId()), hasta.capture());

        assertThat(hasta.getValue()).isEqualTo(respuesta.reservadoHasta());
        verify(eventos).debitoVenta(venta);
        verifyNoInteractions(boletos);
        verify(eventos, never()).boletoVendido(any(), any(), any());
    }

    @Test
    void cancelarUnaVentaPagadaConCarteraPideElReembolso() {
        VentaEntidad venta
                = DatosPrueba.ventaEnLineaConfirmada(
                        viaje, UUID.randomUUID(), 2);
        laVentaExiste(venta);
        elViajeEstaEnLaVista(viaje);
        guardarDevuelveElMismo();
        List<BoletoEntidad> suyos = boletosDe(venta, 5, 6);

        VentaRespuesta respuesta = servicio.cancelar(
                venta.getId(), cancelacion(0), DatosPrueba.adminSistema());

        assertThat(respuesta.estado()).isEqualTo(EstadoVenta.CANCELADA);
        assertThat(respuesta.boletos())
                .extracting(BoletoRespuesta::estado)
                .containsOnly(EstadoBoleto.CANCELADO);

        verify(asientos).liberarDeVenta(venta.getId());

        InOrder orden = inOrder(repositorio, eventos);
        orden.verify(repositorio).saveAndFlush(venta);
        orden.verify(eventos).ventaCancelada(venta, suyos, viaje);
        orden.verify(eventos).reembolsoVenta(
                venta, MotivoSaga.VENTA_CANCELADA, FECHA_PAGO.plusDays(1));
    }

    @Test
    void viajeEliminadoRechazaLaVentaPendienteYLiberaSusAsientos() {
        VentaEntidad pendiente
                = DatosPrueba.ventaEnLinea(viaje, UUID.randomUUID(), 2);
        elViajeEstaEnLaVista(viaje);

        when(repositorio.findByViajeIdAndEstadoIn(
                viaje.getViajeId(), AFECTADAS))
                .thenReturn(List.of(pendiente));

        guardarDevuelveElMismo();

        servicio.compensarViajeEliminado(
                viaje.getViajeId(), "Bus averiado");

        assertThat(pendiente.getEstado()).isEqualTo(EstadoVenta.RECHAZADA);
        assertThat(pendiente.getMotivoRechazo()).isEqualTo("VIAJE_ELIMINADO");

        verify(repositorio).saveAndFlush(pendiente);
        verify(asientos).liberarDeVenta(pendiente.getId());
        verifyNoInteractions(boletos, eventos);
    }

    @Test
    void viajeEliminadoCancelaLaVentaEnEfectivoSinReembolso() {
        VentaEntidad confirmada = DatosPrueba.ventaMostrador(viaje, 2);
        LocalDate fechaEsperada
                = confirmada.fechaNoAnteriorAlPago(Fechas.hoy());
        elViajeEstaEnLaVista(viaje);

        when(repositorio.findByViajeIdAndEstadoIn(
                viaje.getViajeId(), AFECTADAS))
                .thenReturn(List.of(confirmada));

        guardarDevuelveElMismo();
        List<BoletoEntidad> suyos = boletosDe(confirmada, 1, 2);

        servicio.compensarViajeEliminado(
                viaje.getViajeId(), "Bus averiado");

        assertThat(confirmada.getEstado()).isEqualTo(EstadoVenta.CANCELADA);
        assertThat(confirmada.getMotivoCancelacion()).isEqualTo("Bus averiado");
        assertThat(confirmada.getFechaCancelacion()).isEqualTo(fechaEsperada);
        assertThat(suyos)
                .extracting(BoletoEntidad::getEstado)
                .containsOnly(EstadoBoleto.CANCELADO);

        verify(repositorio).saveAndFlush(confirmada);
        verify(boletos).saveAll(suyos);
        verify(asientos).liberarDeVenta(confirmada.getId());
        verify(eventos).ventaCancelada(confirmada, suyos, viaje);
        verify(eventos, never()).reembolsoVenta(any(), any(), any());
    }

    @Test
    void viajeEliminadoCancelaLaVentaConCarteraYPideReembolso() {
        VentaEntidad confirmada
                = DatosPrueba.ventaEnLineaConfirmada(
                        viaje, UUID.randomUUID(), 1);
        elViajeEstaEnLaVista(viaje);

        when(repositorio.findByViajeIdAndEstadoIn(
                viaje.getViajeId(), AFECTADAS))
                .thenReturn(List.of(confirmada));

        guardarDevuelveElMismo();
        List<BoletoEntidad> suyos = boletosDe(confirmada, 9);

        servicio.compensarViajeEliminado(
                viaje.getViajeId(), "Bus averiado");

        assertThat(confirmada.getEstado()).isEqualTo(EstadoVenta.CANCELADA);
        assertThat(confirmada.getFechaCancelacion())
                .isAfterOrEqualTo(FECHA_PAGO);

        verify(eventos).ventaCancelada(confirmada, suyos, viaje);
        verify(eventos).reembolsoVenta(
                confirmada, MotivoSaga.VIAJE_ELIMINADO,
                confirmada.getFechaCancelacion());
    }

    @Test
    void liberarReservasRechazaPrimeroLasVentasYLuegoLiberaLosAsientos() {
        when(repositorio.rechazarPendientesVencidas(MINUTOS_RESERVA))
                .thenReturn(2);
        when(asientos.liberarReservasVencidas()).thenReturn(3);

        assertThat(servicio.liberarReservasVencidas()).isEqualTo(2);

        InOrder orden = inOrder(repositorio, asientos);
        orden.verify(repositorio)
                .rechazarPendientesVencidas(MINUTOS_RESERVA);
        orden.verify(asientos).liberarReservasVencidas();
    }

    @Test
    void obtenerUnaVentaQueNoExiste() {
        UUID id = UUID.randomUUID();
        when(repositorio.findById(id)).thenReturn(Optional.empty());

        VerificarError.lanza(
                () -> servicio.obtener(id, adminXela),
                ErrorVenta.VENTA_NO_EXISTE);
    }

    @Test
    void unaVentaAjenaNoSePuedeVer() {
        VentaEntidad mostrador = DatosPrueba.ventaMostrador(viaje, 1);
        VentaEntidad enLinea
                = DatosPrueba.ventaEnLineaConfirmada(
                        viaje, UUID.randomUUID(), 1);
        laVentaExiste(mostrador);
        laVentaExiste(enLinea);

        VerificarError.lanza(
                () -> servicio.obtener(
                        mostrador.getId(), DatosPrueba.cliente()),
                ErrorVenta.VENTA_AJENA);

        VerificarError.lanza(
                () -> servicio.obtener(
                        enLinea.getId(), DatosPrueba.cliente()),
                ErrorVenta.VENTA_AJENA);

        VerificarError.lanza(
                () -> servicio.obtener(
                        enLinea.getId(), DatosPrueba.cajero(GUATE)),
                ErrorVenta.VENTA_AJENA);

        VerificarError.lanza(
                () -> servicio.obtener(
                        mostrador.getId(), DatosPrueba.adminSucursal(GUATE)),
                ErrorVenta.VENTA_AJENA);

        verifyNoInteractions(boletos, viajeRepositorio);
    }

    @Test
    void elClienteDuenoVeSuVentaConBoletosYViaje() {
        UUID clienteId = UUID.randomUUID();
        VentaEntidad venta
                = DatosPrueba.ventaEnLineaConfirmada(viaje, clienteId, 2);
        laVentaExiste(venta);
        elViajeEstaEnLaVista(viaje);
        boletosDe(venta, 3, 4);

        VentaRespuesta respuesta
                = servicio.obtener(venta.getId(), DatosPrueba.cliente(clienteId));

        assertThat(respuesta.ventaId()).isEqualTo(venta.getId());
        assertThat(respuesta.estado()).isEqualTo(EstadoVenta.CONFIRMADA);
        assertThat(respuesta.canal()).isEqualTo(CanalVenta.EN_LINEA);
        assertThat(respuesta.version()).isZero();
        assertThat(respuesta.boletos())
                .extracting(BoletoRespuesta::numeroAsiento)
                .containsExactly(3, 4);
        assertThat(respuesta.boletos())
                .extracting(BoletoRespuesta::boletoId)
                .doesNotContainNull();
        assertThat(respuesta.viaje()).isNotNull();
        assertThat(respuesta.viaje().busPlaca()).isEqualTo("P-123ABC");
    }

    @Test
    void listarAgrupaLosBoletosDeCadaVentaOrdenadosPorAsiento() {
        VentaEntidad primera = DatosPrueba.ventaMostrador(viaje, 2);
        VentaEntidad segunda
                = DatosPrueba.ventaEnLineaConfirmada(
                        viaje, UUID.randomUUID(), 1);
        VentaEntidad sinBoletos
                = DatosPrueba.ventaEnLinea(viaje, UUID.randomUUID(), 1);

        when(repositorio.findAll(
                ArgumentMatchers.<Specification<VentaEntidad>>any(),
                any(Sort.class)))
                .thenReturn(List.of(primera, segunda, sinBoletos));

        List<BoletoEntidad> todos = new ArrayList<>();
        todos.addAll(DatosPrueba.boletos(segunda, 2));
        todos.addAll(DatosPrueba.boletos(primera, 9, 4));

        when(boletos.findByVentaIdIn(
                List.of(primera.getId(), segunda.getId(), sinBoletos.getId())))
                .thenReturn(todos);

        List<VentaRespuesta> respuestas = servicio.listar(
                new FiltroVentas(null, XELA, null, null, null, null));

        assertThat(respuestas)
                .extracting(VentaRespuesta::ventaId)
                .containsExactly(
                        primera.getId(), segunda.getId(), sinBoletos.getId());

        assertThat(respuestas.get(0).boletos())
                .extracting(BoletoRespuesta::numeroAsiento)
                .containsExactly(4, 9);
        assertThat(respuestas.get(1).boletos())
                .extracting(BoletoRespuesta::numeroAsiento)
                .containsExactly(2);
        assertThat(respuestas.get(2).boletos()).isEmpty();
        assertThat(respuestas)
                .allSatisfy(r -> assertThat(r.viaje()).isNull());

        verifyNoInteractions(viajeRepositorio);
        verify(repositorio).findAll(
                ArgumentMatchers.<Specification<VentaEntidad>>any(),
                eq(Sort.by(
                        Sort.Order.desc("fechaPago"),
                        Sort.Order.desc("id"))));
    }

    @Test
    void enLineaCuandoTripsDiceQueNoSeVende() {
        UsuarioActual cliente = DatosPrueba.cliente();
        tienePerfil(cliente);
        viajeALaVenta();
        tripsDiceVendible(false, "VIAJE_PRIVADO");

        VerificarError.lanza(
                () -> servicio.venderEnLinea(enLinea(List.of(1)), cliente),
                ErrorViaje.VIAJE_NO_VENDIBLE);

        verifyNoInteractions(repositorio, asientos, boletos, eventos);
    }

    @Test
    void mostradorConNitDeClienteRegistradoCompletaNombreYCorreo() {
        viajeALaVenta();
        tripsDiceVendible(true, null);
        guardarDevuelveElMismo();

        when(clientes.findFirstByNit("1234567-K")).thenReturn(Optional.of(
                DatosPrueba.vistaCliente(
                        UUID.randomUUID(),
                        "1234567-K",
                        "Ana Lopez",
                        "ana@correo.com")));

        when(asientos.vender(any(), any(), any())).thenReturn(1);

        NuevaVentaMostradorSolicitud solicitud
                = new NuevaVentaMostradorSolicitud(
                        viaje.getViajeId(),
                        List.of(7),
                        " 1234567-k ",
                        null,
                        null,
                        FECHA_PAGO);

        VentaRespuesta respuesta
                = servicio.venderEnMostrador(solicitud, cajeroXela);

        VentaEntidad venta = ventaGuardada();

        assertThat(respuesta.clienteNit()).isEqualTo("1234567-K");
        assertThat(respuesta.clienteNombre()).isEqualTo("Ana Lopez");
        assertThat(venta.getClienteCorreo()).isEqualTo("ana@correo.com");
        assertThat(venta.getClienteUsuarioId()).isNull();

        verify(clientes).findFirstByNit("1234567-K");
    }
}
