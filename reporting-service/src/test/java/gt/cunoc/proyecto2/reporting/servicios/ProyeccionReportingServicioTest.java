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
import gt.cunoc.proyecto2.reporting.modelos.GastoEntidad;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProyeccionReportingServicioTest {

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
    void boletoVendidoGuardaIngresoUnaSolaVez() {
        UUID eventoId = UUID.randomUUID();
        UUID sucursalId = UUID.randomUUID();
        when(ingresos.existsByEventoOrigenId(eventoId)).thenReturn(false, true);
        BoletoVendido evento = new BoletoVendido(UUID.randomUUID(), UUID.randomUUID(), null,
                null, sucursalId, null, "CF", "Consumidor final", 2,
                new BigDecimal("100.00"), LocalDate.of(2026, 1, 5), "Xela", "Guate", null);

        proyeccion.aplicarBoletoVendido(eventoId, evento, LocalDate.of(2026, 1, 5));
        proyeccion.aplicarBoletoVendido(eventoId, evento, LocalDate.of(2026, 1, 5));

        ArgumentCaptor<IngresoEntidad> guardados = ArgumentCaptor.forClass(IngresoEntidad.class);
        verify(ingresos).save(guardados.capture());
        assertThat(guardados.getValue().getMonto()).isEqualByComparingTo("100.00");
        assertThat(guardados.getValue().getTipo()).isEqualTo("BOLETO");
    }

    @Test
    void boletoRepetidoNoSeGuardaDosVeces() {
        UUID eventoId = UUID.randomUUID();
        when(ingresos.existsByEventoOrigenId(eventoId)).thenReturn(true);
        BoletoVendido evento = new BoletoVendido(UUID.randomUUID(), null, null,
                null, UUID.randomUUID(), null, "CF", "CF", 1,
                new BigDecimal("50.00"), LocalDate.now(), null, null, null);

        proyeccion.aplicarBoletoVendido(eventoId, evento, LocalDate.now());

        verify(ingresos, never()).save(any());
    }

    @Test
    void cancelacionCopiaDatosDeLaFilaPositivaConMontosNegativos() {
        UUID eventoId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();
        UUID rutaId = UUID.randomUUID();
        UUID busId = UUID.randomUUID();
        when(ingresos.existsByEventoOrigenId(eventoId)).thenReturn(false);
        IngresoEntidad positiva = new IngresoEntidad();
        positiva.setSucursalId(UUID.randomUUID());
        positiva.setRutaId(rutaId);
        positiva.setBusId(busId);
        positiva.setOrigen("Xela");
        positiva.setDestino("Guate");
        when(ingresos.filasPositivasDe(ventaId)).thenReturn(List.of(positiva));
        VentaCancelada evento = new VentaCancelada(ventaId, null, null, "CF", "CF", 2,
                new BigDecimal("100.00"));

        proyeccion.aplicarVentaCancelada(eventoId, evento, LocalDate.of(2026, 1, 6));

        ArgumentCaptor<IngresoEntidad> guardados = ArgumentCaptor.forClass(IngresoEntidad.class);
        verify(ingresos).save(guardados.capture());
        assertThat(guardados.getValue().getMonto()).isEqualByComparingTo("-100.00");
        assertThat(guardados.getValue().getCantidadBoletos()).isEqualTo(-2);
        assertThat(guardados.getValue().getRutaId()).isEqualTo(rutaId);
        assertThat(guardados.getValue().getBusId()).isEqualTo(busId);
    }

    @Test
    void cancelacionSinVentaPreviaLanzaParaReintentar() {
        UUID eventoId = UUID.randomUUID();
        UUID ventaId = UUID.randomUUID();
        when(ingresos.existsByEventoOrigenId(eventoId)).thenReturn(false);
        when(ingresos.filasPositivasDe(ventaId)).thenReturn(List.of());
        VentaCancelada evento = new VentaCancelada(ventaId, null, null, "CF", "CF", 1,
                new BigDecimal("50.00"));

        assertThatThrownBy(() -> proyeccion.aplicarVentaCancelada(eventoId, evento, LocalDate.now()))
                .isInstanceOf(IllegalStateException.class);
        verify(ingresos, never()).save(any());
    }

    @Test
    void viajeCompletadoGeneraTresGastos() {
        UUID eventoId = UUID.randomUUID();
        UUID viajeId = UUID.randomUUID();
        UUID busId = UUID.randomUUID();
        UUID sucursalId = UUID.randomUUID();
        ViajeEntidad viaje = new ViajeEntidad();
        viaje.setViajeId(viajeId);
        viaje.setBusId(busId);
        viaje.setSucursalId(sucursalId);
        viaje.setEstado("EN_RUTA");
        when(viajes.findById(viajeId)).thenReturn(Optional.of(viaje));
        when(buses.findById(busId)).thenReturn(Optional.empty());

        ViajeCompletado evento = new ViajeCompletado(viajeId, "REGULAR", null, sucursalId, busId, null,
                new BigDecimal("50"), new BigDecimal("200"), new BigDecimal("1.5"),
                new BigDecimal("75"), new BigDecimal("120"), null);

        proyeccion.aplicarViajeCompletado(eventoId, evento, 1L, LocalDate.of(2026, 2, 1));

        verify(viajes).save(any(ViajeEntidad.class));
        verify(gastos, org.mockito.Mockito.times(3)).save(any());
        assertThat(viaje.getEstado()).isEqualTo("COMPLETADO");
        assertThat(viaje.getKmRecorridos()).isEqualByComparingTo("50");
    }

    @Test
    void viajeCompletadoSinViajeLanzaParaReintentar() {
        UUID viajeId = UUID.randomUUID();
        when(viajes.findById(viajeId)).thenReturn(Optional.empty());
        ViajeCompletado evento = new ViajeCompletado(viajeId, "REGULAR", null, UUID.randomUUID(),
                UUID.randomUUID(), null, BigDecimal.ONE, BigDecimal.TEN, BigDecimal.ONE,
                BigDecimal.TEN, BigDecimal.TEN, null);

        assertThatThrownBy(() -> proyeccion.aplicarViajeCompletado(
                UUID.randomUUID(), evento, 1L, LocalDate.now()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void marcarEnRutaSinViajeLanzaParaReintentar() {
        UUID viajeId = UUID.randomUUID();
        when(viajes.findById(viajeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> proyeccion.marcarViajeEnRuta(viajeId, UUID.randomUUID(), 1L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void tarifaNuevaSeGuardaDuplicadaSeIgnora() {
        UUID tarifaId = UUID.randomUUID();
        when(tarifas.existsById(tarifaId)).thenReturn(false, true);
        var evento = new TarifaDepreciacionCambiada(tarifaId,
                new BigDecimal("1.25"), LocalDate.of(2026, 10, 1));

        proyeccion.aplicarTarifa(evento);
        proyeccion.aplicarTarifa(evento);
        proyeccion.aplicarTarifa(null);
        proyeccion.aplicarTarifa(new TarifaDepreciacionCambiada(null, null, null));

        verify(tarifas).save(any());
    }

    @Test
    void sucursalCreaActualizaYDescartaVersionVieja() {
        UUID id = UUID.randomUUID();
        when(sucursales.findById(id)).thenReturn(Optional.empty());
        var evento = new SucursalEvento(id, "C", "Norte", "D", "M",
                BigDecimal.ONE, BigDecimal.ONE, true);
        proyeccion.aplicarSucursal(evento, 0L);

        SucursalEntidad guardada = new SucursalEntidad();
        guardada.setId(id);
        guardada.setNombre("Vieja");
        guardada.setVersion(3);
        when(sucursales.findById(id)).thenReturn(Optional.of(guardada));
        proyeccion.aplicarSucursal(evento, 5L);
        assertThat(guardada.getVersion()).isEqualTo(5);
        proyeccion.aplicarSucursal(evento, 0L); // vieja: se descarta
        assertThat(guardada.getVersion()).isEqualTo(5);

        verify(sucursales, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void busCreaActualizaDesactivaYDescartaVersionVieja() {
        UUID id = UUID.randomUUID();
        when(buses.findById(id)).thenReturn(Optional.empty());
        var evento = new BusEvento(id, UUID.randomUUID(), "P1", null, null, null, null, null);
        proyeccion.aplicarBus(evento, 0L);

        BusEntidad guardado = new BusEntidad();
        guardado.setId(id);
        guardado.setVersion(3);
        when(buses.findById(id)).thenReturn(Optional.of(guardado));
        proyeccion.aplicarBus(evento, 3L);
        proyeccion.aplicarBus(evento, 0L); // vieja
        proyeccion.desactivarBus(id, 9L);
        assertThat(guardado.getEstado()).isEqualTo("INACTIVO");
        assertThat(guardado.getVersion()).isEqualTo(9);
        guardado.setVersion(20);
        proyeccion.desactivarBus(id, 0L); // vieja
        proyeccion.desactivarBus(UUID.randomUUID(), 1L); // no existe

        verify(buses, org.mockito.Mockito.times(3)).save(any());
    }

    @Test
    void choferCreaActualizaDesactivaYDescartaVersionVieja() {
        UUID id = UUID.randomUUID();
        when(choferes.findById(id)).thenReturn(Optional.empty());
        var evento = new ChoferEvento(id, UUID.randomUUID(), UUID.randomUUID(), "N", "L",
                null, LocalDate.of(2027, 1, 1), null, null);
        proyeccion.aplicarChofer(evento, 0L);

        ChoferEntidad guardado = new ChoferEntidad();
        guardado.setId(id);
        guardado.setVersion(3);
        when(choferes.findById(id)).thenReturn(Optional.of(guardado));
        proyeccion.aplicarChofer(evento, 5L);
        proyeccion.aplicarChofer(evento, 0L); // vieja
        proyeccion.desactivarChofer(id, 7L);
        assertThat(guardado.getEstado()).isEqualTo("INACTIVO");
        assertThat(guardado.getVersion()).isEqualTo(7);
        guardado.setVersion(20);
        proyeccion.desactivarChofer(id, 0L); // vieja
        proyeccion.desactivarChofer(UUID.randomUUID(), 1L); // no existe

        verify(choferes, org.mockito.Mockito.times(3)).save(any());
    }

    @Test
    void rutaAplicaConNombresDeSucursalYSeElimina() {
        UUID id = UUID.randomUUID();
        UUID origenId = UUID.randomUUID();
        SucursalEntidad origen = new SucursalEntidad();
        origen.setId(origenId);
        origen.setNombre("Origen");
        when(rutas.findById(id)).thenReturn(Optional.empty());
        when(sucursales.findById(any(UUID.class))).thenAnswer(inv -> {
            UUID pedido = inv.getArgument(0);
            return pedido.equals(origenId) ? Optional.of(origen) : Optional.empty();
        });
        var evento = new RutaEvento(id, origenId, UUID.randomUUID(), null, null,
                new BigDecimal("10"), new BigDecimal("20"), null);

        proyeccion.aplicarRuta(evento);

        ArgumentCaptor<RutaEntidad> captor = ArgumentCaptor.forClass(RutaEntidad.class);
        verify(rutas).save(captor.capture());
        assertThat(captor.getValue().getOrigenNombre()).isEqualTo("Origen");
        assertThat(captor.getValue().getDestinoNombre()).isEqualTo("-");
        assertThat(captor.getValue().isActiva()).isTrue();

        proyeccion.eliminarRuta(id);
        verify(rutas).deleteById(id);
    }

    @Test
    void viajeProgramadoActualizadoEliminadoYChoferActual() {
        UUID viajeId = UUID.randomUUID();
        UUID busId = UUID.randomUUID();
        UUID busNuevo = UUID.randomUUID();
        when(viajes.findById(viajeId)).thenReturn(Optional.empty());
        var programado = new ViajeEvento(viajeId, null, null, UUID.randomUUID(), busId,
                UUID.randomUUID(), null);
        proyeccion.aplicarViaje(programado, 0L);

        ViajeEntidad guardado = new ViajeEntidad();
        guardado.setViajeId(viajeId);
        guardado.setBusId(busId);
        guardado.setEstado("PROGRAMADO");
        when(viajes.findById(viajeId)).thenReturn(Optional.of(guardado));
        var actualizado = new ViajeEvento(viajeId, "PRIVADO", null, UUID.randomUUID(), busNuevo,
                UUID.randomUUID(), null);
        proyeccion.aplicarViajeActualizado(actualizado, 1L);
        proyeccion.aplicarViaje(actualizado, 0L); // versión vieja
        assertThat(guardado.getVersion()).isEqualTo(1);
        guardado.setEstado("COMPLETADO");
        proyeccion.aplicarViaje(actualizado, 9L); // completado no revive

        proyeccion.eliminarViaje(viajeId);
        proyeccion.eliminarViaje(UUID.randomUUID()); // sin bus
        proyeccion.recalcularChoferActual(null); // sin bus
        proyeccion.marcarViajeEnRuta(viajeId, busId, 0L); // no programado
        guardado.setEstado("PROGRAMADO");
        proyeccion.marcarViajeEnRuta(viajeId, busId, 0L); // vieja
        proyeccion.marcarViajeEnRuta(viajeId, null, 5L);
        assertThat(guardado.getEstado()).isEqualTo("EN_RUTA");
        assertThat(guardado.getVersion()).isEqualTo(5);

        verify(viajes, org.mockito.Mockito.atLeastOnce()).save(any());
        verify(viajes).deleteById(viajeId);
    }

    @Test
    void viajeCompletadoRepetidoNoDuplicaGastos() {
        UUID eventoId = UUID.randomUUID();
        ViajeEntidad viaje = new ViajeEntidad();
        viaje.setViajeId(UUID.randomUUID());
        viaje.setBusId(UUID.randomUUID());
        viaje.setSucursalId(UUID.randomUUID());
        viaje.setEstado("COMPLETADO");
        when(viajes.findById(viaje.getViajeId())).thenReturn(Optional.of(viaje));
        when(buses.findById(any())).thenReturn(Optional.empty());
        var evento = new ViajeCompletado(viaje.getViajeId(), "REGULAR", null, viaje.getSucursalId(),
                viaje.getBusId(), null, null, new BigDecimal("10"), null, new BigDecimal("5"),
                null, null);

        proyeccion.aplicarViajeCompletado(eventoId, evento, 9L, LocalDate.now());

        verify(gastos, org.mockito.Mockito.times(2)).save(any());
    }

    @Test
    void alquilerSinDatosSeIgnoraYDuplicadoNoDuplica() {
        UUID eventoId = UUID.randomUUID();
        when(ingresos.existsByEventoOrigenId(eventoId)).thenReturn(false, true);
        var sinDatos = new AlquilerPagado(null, null, null, null, null, null, null, null, null, null, null, null);
        proyeccion.aplicarAlquilerPagado(eventoId, sinDatos, LocalDate.now());
        var ok = new AlquilerPagado(UUID.randomUUID(), null, UUID.randomUUID(), "CF", "CF",
                "X", "Y", null, null, new BigDecimal("100"), null, null);
        proyeccion.aplicarAlquilerPagado(eventoId, ok, null);
        proyeccion.aplicarAlquilerPagado(eventoId, ok, null);

        verify(ingresos).save(any());
    }

    @Test
    void tallerGuardaUnaFilaPorLinea() {
        UUID eventoId = UUID.randomUUID();
        UUID sucursalId = UUID.randomUUID();
        UUID busId = UUID.randomUUID();
        var linea = new GastoTallerRegistrado.Linea(UUID.randomUUID(), UUID.randomUUID(),
                "Llantas", "Cambio", "REPUESTO", new BigDecimal("6800.00"));
        var evento = new GastoTallerRegistrado(UUID.randomUUID(), busId, sucursalId,
                LocalDate.of(2026, 10, 20), List.of(linea));

        proyeccion.aplicarGastoTaller(eventoId, evento, LocalDate.of(2026, 10, 21));

        ArgumentCaptor<GastoEntidad> captor = ArgumentCaptor.forClass(GastoEntidad.class);
        verify(gastos).save(captor.capture());
        assertThat(captor.getValue().getMonto()).isEqualByComparingTo("6800.00");
        assertThat(captor.getValue().getFechaNegocio()).isEqualTo(LocalDate.of(2026, 10, 20));
        assertThat(captor.getValue().getTipo()).isEqualTo("REPUESTO");
    }

    @Test
    void tallerSinDatosSeIgnoraLineaNulaSeSalta() {
        UUID eventoId = UUID.randomUUID();
        proyeccion.aplicarGastoTaller(eventoId,
                new GastoTallerRegistrado(null, null, null, null, null), LocalDate.now());
        proyeccion.aplicarGastoTaller(eventoId,
                new GastoTallerRegistrado(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                        LocalDate.of(2026, 10, 20), List.of()), LocalDate.now());
        var lineaNula = new GastoTallerRegistrado(UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), null, java.util.Arrays.asList(null,
                        new GastoTallerRegistrado.Linea(null, null, null, null, null, null)));
        proyeccion.aplicarGastoTaller(eventoId, lineaNula, LocalDate.of(2026, 10, 20));

        verify(gastos, never()).save(any());
    }

    @Test
    void categoriaEnBlancoSeIgnoraYConDatosActualiza() {
        proyeccion.aplicarCategoriaActualizada(null, "X");
        proyeccion.aplicarCategoriaActualizada(UUID.randomUUID(), "  ");
        UUID categoriaId = UUID.randomUUID();
        GastoEntidad gasto = new GastoEntidad();
        when(gastos.findByCategoriaIdOrderByFechaNegocioDesc(categoriaId))
                .thenReturn(List.of(gasto));
        proyeccion.aplicarCategoriaActualizada(categoriaId, "Motor");

        assertThat(gasto.getCategoriaNombre()).isEqualTo("Motor");
        verify(gastos).saveAll(any());
    }

    // ----- version = aggregateVersion del origen (no @Version de JPA) -----

    /** ViajeCompletado de un viaje EN_RUTA del bus: sube total_viajes y recalcula el chofer actual. */
    private void completarViaje(UUID busId, UUID choferId, long versionViaje) {
        ViajeEntidad viaje = new ViajeEntidad();
        viaje.setViajeId(UUID.randomUUID());
        viaje.setBusId(busId);
        viaje.setChoferId(choferId);
        viaje.setSucursalId(UUID.randomUUID());
        viaje.setEstado("EN_RUTA");
        when(viajes.findById(viaje.getViajeId())).thenReturn(Optional.of(viaje));
        var evento = new ViajeCompletado(viaje.getViajeId(), "REGULAR", null, viaje.getSucursalId(),
                busId, choferId, new BigDecimal("200"), new BigDecimal("400"), new BigDecimal("1.25"),
                new BigDecimal("250"), new BigDecimal("350"), null);
        proyeccion.aplicarViajeCompletado(UUID.randomUUID(), evento, versionViaje, LocalDate.of(2026, 10, 20));
        assertThat(viaje.getVersion()).isEqualTo((int) versionViaje);
    }

    @Test
    void busConViajesLocalesAceptaBusActualizadoConLaSiguienteVersion() {
        UUID busId = UUID.randomUUID();
        UUID sucursalId = UUID.randomUUID();
        UUID choferProximo = UUID.randomUUID();
        BusEntidad bus = new BusEntidad();
        bus.setId(busId);
        bus.setSucursalId(sucursalId);
        bus.setPlaca("C123ABC");
        bus.setEstado("ACTIVO");
        bus.setVersion(2); // último BusActualizado de Fleet
        when(buses.findById(busId)).thenReturn(Optional.of(bus));
        when(viajes.choferActualDe(any(), any())).thenReturn(List.of(choferProximo));

        completarViaje(busId, null, 4L);
        completarViaje(busId, null, 4L);
        completarViaje(busId, null, 4L);

        // Los cambios locales no tocan la versión de Fleet
        assertThat(bus.getTotalViajes()).isEqualTo(3);
        assertThat(bus.getChoferActualId()).isEqualTo(choferProximo);
        assertThat(bus.getVersion()).isEqualTo(2);

        proyeccion.aplicarBus(new BusEvento(busId, sucursalId, "C123ABC", "Volvo", "B12", 50,
                "EN_TALLER", new BigDecimal("1500.00")), 3L);

        assertThat(bus.getEstado()).isEqualTo("EN_TALLER");
        assertThat(bus.getKilometrajeActual()).isEqualByComparingTo("1500.00");
        assertThat(bus.getVersion()).isEqualTo(3);
        assertThat(bus.getTotalViajes()).isEqualTo(3);
        assertThat(bus.getChoferActualId()).isEqualTo(choferProximo);
    }

    @Test
    void busActualizadoConVersionMenorSeDescartaEIgualSeAplica() {
        UUID busId = UUID.randomUUID();
        BusEntidad bus = new BusEntidad();
        bus.setId(busId);
        bus.setPlaca("C123ABC");
        bus.setEstado("ACTIVO");
        bus.setTotalViajes(4);
        bus.setVersion(5);
        when(buses.findById(busId)).thenReturn(Optional.of(bus));
        var evento = new BusEvento(busId, UUID.randomUUID(), "C999ZZZ", "Volvo", "B12", 50,
                "EN_TALLER", null);

        proyeccion.aplicarBus(evento, 4L); // vieja: se descarta
        proyeccion.desactivarBus(busId, 4L); // vieja: se descarta

        assertThat(bus.getPlaca()).isEqualTo("C123ABC");
        assertThat(bus.getEstado()).isEqualTo("ACTIVO");
        assertThat(bus.getVersion()).isEqualTo(5);
        verify(buses, never()).save(any());

        proyeccion.aplicarBus(evento, 5L); // misma versión (republicación): se aplica
        assertThat(bus.getPlaca()).isEqualTo("C999ZZZ");
        assertThat(bus.getVersion()).isEqualTo(5);
        assertThat(bus.getTotalViajes()).isEqualTo(4);
        verify(buses).save(bus);
    }

    @Test
    void choferDesactivadoDespuesDeVariosViajesSeAplica() {
        UUID choferId = UUID.randomUUID();
        ChoferEntidad chofer = new ChoferEntidad();
        chofer.setId(choferId);
        chofer.setEstado("ACTIVO");
        chofer.setVersion(2); // ChoferCreado (1) + ChoferActualizado (2)
        when(choferes.findById(choferId)).thenReturn(Optional.of(chofer));

        UUID busId = UUID.randomUUID();
        completarViaje(busId, choferId, 4L);
        completarViaje(busId, choferId, 4L);
        completarViaje(busId, choferId, 4L);
        assertThat(chofer.getTotalViajes()).isEqualTo(3);
        assertThat(chofer.getVersion()).isEqualTo(2);

        proyeccion.desactivarChofer(choferId, 3L);

        assertThat(chofer.getEstado()).isEqualTo("INACTIVO");
        assertThat(chofer.getVersion()).isEqualTo(3);
        assertThat(chofer.getTotalViajes()).isEqualTo(3);
    }

    @Test
    void eventoSinVersionSeAplicaYNoBajaLaVersionGuardada() {
        UUID busId = UUID.randomUUID();
        BusEntidad bus = new BusEntidad();
        bus.setId(busId);
        bus.setPlaca("C123ABC");
        bus.setEstado("ACTIVO");
        bus.setVersion(7);
        when(buses.findById(busId)).thenReturn(Optional.of(bus));

        proyeccion.aplicarBus(new BusEvento(busId, UUID.randomUUID(), "C999ZZZ", null, null, null,
                "EN_TALLER", null), null);
        assertThat(bus.getPlaca()).isEqualTo("C999ZZZ");
        assertThat(bus.getEstado()).isEqualTo("EN_TALLER");
        assertThat(bus.getVersion()).isEqualTo(7);

        proyeccion.desactivarBus(busId, null);
        assertThat(bus.getEstado()).isEqualTo("INACTIVO");
        assertThat(bus.getVersion()).isEqualTo(7);

        UUID choferId = UUID.randomUUID();
        ChoferEntidad chofer = new ChoferEntidad();
        chofer.setId(choferId);
        chofer.setEstado("ACTIVO");
        chofer.setVersion(4);
        when(choferes.findById(choferId)).thenReturn(Optional.of(chofer));
        proyeccion.desactivarChofer(choferId, null);
        assertThat(chofer.getEstado()).isEqualTo("INACTIVO");
        assertThat(chofer.getVersion()).isEqualTo(4);

        // Alta sin versión: la columna NOT NULL queda en 0
        UUID nuevoId = UUID.randomUUID();
        when(buses.findById(nuevoId)).thenReturn(Optional.empty());
        proyeccion.aplicarBus(new BusEvento(nuevoId, UUID.randomUUID(), "P1", null, null, null, null, null),
                null);
        ArgumentCaptor<BusEntidad> captor = ArgumentCaptor.forClass(BusEntidad.class);
        verify(buses, org.mockito.Mockito.times(3)).save(captor.capture());
        BusEntidad nuevo = captor.getAllValues().get(2);
        assertThat(nuevo.getId()).isEqualTo(nuevoId);
        assertThat(nuevo.getVersion()).isZero();
    }

    @Test
    void altaGuardaLaVersionDelEventoYLaDeViajeSigueATrips() {
        UUID busId = UUID.randomUUID();
        when(buses.findById(busId)).thenReturn(Optional.empty());
        proyeccion.aplicarBus(new BusEvento(busId, UUID.randomUUID(), "P1", null, null, null, null, null), 1L);
        ArgumentCaptor<BusEntidad> bus = ArgumentCaptor.forClass(BusEntidad.class);
        verify(buses).save(bus.capture());
        assertThat(bus.getValue().getVersion()).isEqualTo(1);

        UUID viajeId = UUID.randomUUID();
        ViajeEntidad viaje = new ViajeEntidad();
        viaje.setViajeId(viajeId);
        viaje.setBusId(busId);
        viaje.setEstado("PROGRAMADO");
        viaje.setVersion(1);
        when(viajes.findById(viajeId)).thenReturn(Optional.of(viaje));

        proyeccion.marcarViajeEnRuta(viajeId, busId, 3L);
        assertThat(viaje.getEstado()).isEqualTo("EN_RUTA");
        assertThat(viaje.getVersion()).isEqualTo(3);

        var completado = new ViajeCompletado(viajeId, "REGULAR", null, UUID.randomUUID(), busId, null,
                new BigDecimal("10"), null, null, null, null, null);
        proyeccion.aplicarViajeCompletado(UUID.randomUUID(), completado, 2L, LocalDate.now()); // vieja
        assertThat(viaje.getEstado()).isEqualTo("EN_RUTA");
        proyeccion.aplicarViajeCompletado(UUID.randomUUID(), completado, 4L, LocalDate.now());
        assertThat(viaje.getEstado()).isEqualTo("COMPLETADO");
        assertThat(viaje.getVersion()).isEqualTo(4);
    }
}
