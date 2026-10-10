package gt.cunoc.proyecto2.reporting.servicios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.reporting.eventos.recibidos.sales.BoletoVendido;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.sales.VentaCancelada;
import gt.cunoc.proyecto2.reporting.eventos.recibidos.trips.ViajeCompletado;
import gt.cunoc.proyecto2.reporting.modelos.IngresoEntidad;
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
}
