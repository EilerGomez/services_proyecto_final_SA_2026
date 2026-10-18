package gt.cunoc.proyecto2.reporting.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.BusEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.ChoferEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet.SucursalEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.identity.TarifaDepreciacionCambiada;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.maintenance.GastoTallerRegistrado;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.sales.AlquilerPagado;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.sales.BoletoVendido;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.sales.VentaCancelada;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.RutaEvento;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.ViajeCompletado;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.ViajeEvento;
import gt.cunoc.proyecto2.reporting.modelos.BusEntidad;
import gt.cunoc.proyecto2.reporting.modelos.ChoferEntidad;
import gt.cunoc.proyecto2.reporting.modelos.IngresoEntidad;
import gt.cunoc.proyecto2.reporting.modelos.RutaEntidad;
import gt.cunoc.proyecto2.reporting.modelos.SucursalEntidad;
import gt.cunoc.proyecto2.reporting.modelos.ViajeEntidad;
import gt.cunoc.proyecto2.reporting.repositorios.BusRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.ChoferRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.GastoRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.IngresoRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.RutaRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.SucursalRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.TarifaDepreciacionRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.ViajeRepositorio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Ramas borde: nulos, versiones viejas, entidades existentes y fallbacks. */
@ExtendWith(MockitoExtension.class)
class ProyeccionRamasTest {

    @Mock
    SucursalRepositorio sucursales;
    @Mock
    TarifaDepreciacionRepositorio tarifas;
    @Mock
    RutaRepositorio rutas;
    @Mock
    BusRepositorio buses;
    @Mock
    ChoferRepositorio choferes;
    @Mock
    ViajeRepositorio viajes;
    @Mock
    IngresoRepositorio ingresos;
    @Mock
    GastoRepositorio gastos;

    @InjectMocks
    ProyeccionReportingServicioImpl proyeccion;

    @Test
    void tarifaIncompletaSeIgnora() {
        proyeccion.aplicarTarifa(null);
        proyeccion.aplicarTarifa(new TarifaDepreciacionCambiada(null, BigDecimal.ONE, LocalDate.now()));
        proyeccion.aplicarTarifa(new TarifaDepreciacionCambiada(UUID.randomUUID(), null, LocalDate.now()));
        proyeccion.aplicarTarifa(new TarifaDepreciacionCambiada(UUID.randomUUID(), BigDecimal.ONE, null));

        verify(tarifas, never()).save(any());
    }

    @Test
    void choferConNulosUsaDefectos() {
        UUID id = UUID.randomUUID();
        when(choferes.findById(id)).thenReturn(Optional.empty());
        var evento = new ChoferEvento(id, UUID.randomUUID(), UUID.randomUUID(), "N", "L",
                null, null, null, null);

        proyeccion.aplicarChofer(evento, null);

        var captor = org.mockito.ArgumentCaptor.forClass(ChoferEntidad.class);
        verify(choferes).save(captor.capture());
        assertThat(captor.getValue().getTipoLicencia()).isEqualTo("-");
        assertThat(captor.getValue().getEstado()).isEqualTo("ACTIVO");
    }

    @Test
    void rutaExistenteSeActualizaYResuelveNombres() {
        UUID id = UUID.randomUUID();
        UUID origenId = UUID.randomUUID();
        SucursalEntidad origen = new SucursalEntidad();
        origen.setId(origenId);
        origen.setNombre("Origen");
        RutaEntidad guardada = new RutaEntidad();
        guardada.setId(id);
        when(rutas.findById(id)).thenReturn(Optional.of(guardada));
        when(sucursales.findById(any(UUID.class))).thenAnswer(inv ->
                inv.getArgument(0).equals(origenId) ? Optional.of(origen) : Optional.empty());

        proyeccion.aplicarRuta(new RutaEvento(id, origenId, null, null, null,
                new BigDecimal("10"), new BigDecimal("20"), false));

        var captor = org.mockito.ArgumentCaptor.forClass(RutaEntidad.class);
        verify(rutas).save(captor.capture());
        assertThat(captor.getValue().getOrigenNombre()).isEqualTo("Origen");
        assertThat(captor.getValue().getDestinoNombre()).isEqualTo("-");
        assertThat(captor.getValue().isActiva()).isFalse();
    }

    @Test
    void viajeEnRutaSeActualizaYVersionViejaSeDescarta() {
        UUID viajeId = UUID.randomUUID();
        ViajeEntidad enRuta = new ViajeEntidad();
        enRuta.setViajeId(viajeId);
        enRuta.setBusId(UUID.randomUUID());
        enRuta.setEstado("EN_RUTA");
        enRuta.setVersion(5);
        when(viajes.findById(viajeId)).thenReturn(Optional.of(enRuta));
        var evento = new ViajeEvento(viajeId, null, null, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), null);

        proyeccion.aplicarViaje(evento, 9L);
        assertThat(enRuta.getTipo()).isEqualTo("REGULAR");
        proyeccion.aplicarViaje(evento, 1L); // vieja: se descarta
        verify(viajes).save(any(ViajeEntidad.class));
    }

    @Test
    void viajeActualizadoMismoBusNoRecalculaDosVeces() {
        UUID viajeId = UUID.randomUUID();
        UUID busId = UUID.randomUUID();
        ViajeEntidad guardado = new ViajeEntidad();
        guardado.setViajeId(viajeId);
        guardado.setBusId(busId);
        guardado.setEstado("PROGRAMADO");
        when(viajes.findById(viajeId)).thenReturn(Optional.of(guardado));
        when(viajes.choferActualDe(any(), any())).thenReturn(List.of());

        proyeccion.aplicarViajeActualizado(
                new ViajeEvento(viajeId, "REGULAR", null, null, busId, null, null), 1L);

        verify(viajes).save(any(ViajeEntidad.class));
    }

    @Test
    void eliminarViajeSinBusNoRecalcula() {
        when(viajes.findById(any())).thenReturn(Optional.empty());

        proyeccion.eliminarViaje(UUID.randomUUID());

        verify(viajes).deleteById(any(UUID.class));
        verify(viajes, never()).choferActualDe(any(), any());
    }

    @Test
    void marcarEnRutaDescartaVersionViejaYUsaBusDelViaje() {
        UUID viajeId = UUID.randomUUID();
        UUID busId = UUID.randomUUID();
        ViajeEntidad viaje = new ViajeEntidad();
        viaje.setViajeId(viajeId);
        viaje.setBusId(busId);
        viaje.setEstado("PROGRAMADO");
        viaje.setVersion(7);
        when(viajes.findById(viajeId)).thenReturn(Optional.of(viaje));

        proyeccion.marcarViajeEnRuta(viajeId, busId, 1L); // vieja
        assertThat(viaje.getEstado()).isEqualTo("PROGRAMADO");
        proyeccion.marcarViajeEnRuta(viajeId, null, 9L); // bus del viaje
        assertThat(viaje.getEstado()).isEqualTo("EN_RUTA");
    }

    @Test
    void completadoToleraNulosYCuentaViajes() {
        UUID viajeId = UUID.randomUUID();
        UUID busId = UUID.randomUUID();
        UUID choferId = UUID.randomUUID();
        ViajeEntidad viaje = new ViajeEntidad();
        viaje.setViajeId(viajeId);
        viaje.setEstado("EN_RUTA");
        when(viajes.findById(viajeId)).thenReturn(Optional.of(viaje));
        BusEntidad bus = new BusEntidad();
        bus.setId(busId);
        bus.setTotalViajes(2);
        when(buses.findById(busId)).thenReturn(Optional.of(bus));
        ChoferEntidad chofer = new ChoferEntidad();
        chofer.setId(choferId);
        when(choferes.findById(choferId)).thenReturn(Optional.of(chofer));
        var evento = new ViajeCompletado(viajeId, "REGULAR", null, UUID.randomUUID(), busId,
                choferId, null, new BigDecimal("10"), null, new BigDecimal("5"),
                new BigDecimal("20"), null);

        proyeccion.aplicarViajeCompletado(UUID.randomUUID(), evento, 1L, null);

        assertThat(bus.getTotalViajes()).isEqualTo(3);
        assertThat(chofer.getTotalViajes()).isEqualTo(1);
        verify(gastos, org.mockito.Mockito.times(3)).save(any());
    }

    @Test
    void completadoVersionViejaSeDescarta() {
        ViajeEntidad viaje = new ViajeEntidad();
        viaje.setViajeId(UUID.randomUUID());
        viaje.setBusId(UUID.randomUUID());
        viaje.setEstado("EN_RUTA");
        viaje.setVersion(9);
        when(viajes.findById(viaje.getViajeId())).thenReturn(Optional.of(viaje));
        var evento = new ViajeCompletado(viaje.getViajeId(), "REGULAR", null, UUID.randomUUID(),
                viaje.getBusId(), null, null, null, null, null, null, null);

        proyeccion.aplicarViajeCompletado(UUID.randomUUID(), evento, 1L, LocalDate.now());

        assertThat(viaje.getEstado()).isEqualTo("EN_RUTA");
        verify(gastos, never()).save(any());
    }

    @Test
    void boletoIncompletoSeIgnora() {
        proyeccion.aplicarBoletoVendido(UUID.randomUUID(),
                new BoletoVendido(UUID.randomUUID(), null, null, null, null, null, null, null,
                        null, null, null, null, null, null),
                LocalDate.now());
        proyeccion.aplicarBoletoVendido(UUID.randomUUID(),
                new BoletoVendido(UUID.randomUUID(), null, null, null, UUID.randomUUID(), null,
                        null, null, null, null, null, null, null, null),
                LocalDate.now());

        verify(ingresos, never()).save(any());
    }

    @Test
    void boletoConNulosUsaDefectos() {
        UUID sucursalId = UUID.randomUUID();
        when(ingresos.existsByEventoOrigenId(any())).thenReturn(false);
        var evento = new BoletoVendido(UUID.randomUUID(), null, null, null, sucursalId, null,
                null, null, null, new BigDecimal("50"), null, null, null, null);

        proyeccion.aplicarBoletoVendido(UUID.randomUUID(), evento, null);

        var captor = org.mockito.ArgumentCaptor.forClass(IngresoEntidad.class);
        verify(ingresos).save(captor.capture());
        assertThat(captor.getValue().getCantidadBoletos()).isZero();
    }

    @Test
    void cancelacionSinMontoLanza() {
        when(ingresos.existsByEventoOrigenId(any())).thenReturn(false);

        assertThatThrownBy(() -> proyeccion.aplicarVentaCancelada(UUID.randomUUID(),
                new VentaCancelada(UUID.randomUUID(), null, null, null, null, null, null),
                LocalDate.now())).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelacionSinSucursalUsaLaOriginalYSinFechaUsaHoy() {
        UUID ventaId = UUID.randomUUID();
        UUID sucursalId = UUID.randomUUID();
        when(ingresos.existsByEventoOrigenId(any())).thenReturn(false);
        IngresoEntidad original = new IngresoEntidad();
        original.setSucursalId(sucursalId);
        when(ingresos.filasPositivasDe(ventaId)).thenReturn(List.of(original));

        proyeccion.aplicarVentaCancelada(UUID.randomUUID(),
                new VentaCancelada(ventaId, null, null, null, null, null, new BigDecimal("30")), null);

        var captor = org.mockito.ArgumentCaptor.forClass(IngresoEntidad.class);
        verify(ingresos).save(captor.capture());
        assertThat(captor.getValue().getSucursalId()).isEqualTo(sucursalId);
        assertThat(captor.getValue().getCantidadBoletos()).isZero();
        assertThat(captor.getValue().getMonto()).isEqualByComparingTo("-30");
    }

    @Test
    void alquilerUsaMontoTotalYFechaPago() {
        UUID sucursalId = UUID.randomUUID();
        when(ingresos.existsByEventoOrigenId(any())).thenReturn(false);
        var evento = new AlquilerPagado(null, UUID.randomUUID(), sucursalId, null, null, null,
                null, null, null, new BigDecimal("500"), null, LocalDate.of(2026, 10, 5));

        proyeccion.aplicarAlquilerPagado(UUID.randomUUID(), evento, null);

        var captor = org.mockito.ArgumentCaptor.forClass(IngresoEntidad.class);
        verify(ingresos).save(captor.capture());
        assertThat(captor.getValue().getMonto()).isEqualByComparingTo("500");
        assertThat(captor.getValue().getFechaNegocio()).isEqualTo(LocalDate.of(2026, 10, 5));
    }

    @Test
    void alquilerSinMontosSeIgnora() {
        proyeccion.aplicarAlquilerPagado(UUID.randomUUID(),
                new AlquilerPagado(null, null, null, null, null, null, null, null, null, null, null, null),
                LocalDate.now());
        proyeccion.aplicarAlquilerPagado(UUID.randomUUID(),
                new AlquilerPagado(null, null, UUID.randomUUID(), null, null, null, null, null,
                        null, null, null, null),
                LocalDate.now());

        verify(ingresos, never()).save(any());
    }

    @Test
    void tallerToleraFechasYTallerDuplicado() {
        UUID busId = UUID.randomUUID();
        UUID sucursalId = UUID.randomUUID();
        var linea = new GastoTallerRegistrado.Linea(UUID.randomUUID(), null, null, null, null,
                new BigDecimal("10"));
        var evento = new GastoTallerRegistrado(UUID.randomUUID(), busId, sucursalId, null,
                List.of(linea));

        proyeccion.aplicarGastoTaller(UUID.randomUUID(), evento, LocalDate.of(2026, 10, 2));
        proyeccion.aplicarGastoTaller(UUID.randomUUID(), evento, null);

        verify(gastos, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void tallerSinBusIdSeIgnora() {
        proyeccion.aplicarGastoTaller(UUID.randomUUID(),
                new GastoTallerRegistrado(UUID.randomUUID(), null, UUID.randomUUID(), null, null),
                LocalDate.now());

        verify(gastos, never()).save(any());
    }

    @Test
    void recalcularConChoferAsignadoYBusInexistente() {
        UUID busId = UUID.randomUUID();
        UUID choferId = UUID.randomUUID();
        when(viajes.choferActualDe(any(), any())).thenReturn(List.of(choferId));
        BusEntidad bus = new BusEntidad();
        bus.setId(busId);
        when(buses.findById(busId)).thenReturn(Optional.of(bus));
        proyeccion.recalcularChoferActual(busId);
        assertThat(bus.getChoferActualId()).isEqualTo(choferId);

        when(viajes.choferActualDe(any(), any())).thenReturn(List.of());
        proyeccion.recalcularChoferActual(UUID.randomUUID());
        verify(buses, org.mockito.Mockito.times(1)).save(any());
    }

    @Test
    void desactivarSinVersionAplica() {
        UUID busId = UUID.randomUUID();
        BusEntidad bus = new BusEntidad();
        bus.setId(busId);
        when(buses.findById(busId)).thenReturn(Optional.of(bus));

        proyeccion.desactivarBus(busId, null);

        assertThat(bus.getEstado()).isEqualTo("INACTIVO");
    }
}
