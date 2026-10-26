package gt.cunoc.proyecto2.reporting.reportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.reporting.repositorios.BusRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.ChoferRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.GastoRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.IngresoRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.RutaRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.SucursalRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.TarifaDepreciacionRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.ViajeRepositorio;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorBuses;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorChoferes;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorDepreciacion;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorIngresosAlquiler;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorIngresosBoletos;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorIngresosGlobales;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorGastosGlobales;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorGanancias;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorMapaRutas;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorRutasDemandadas;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorTallerBus;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorTallerCategoria;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/** Cada plantilla compila y exporta un PDF válido incluso sin datos. */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GeneradoresPdfTest {

    @Mock
    IngresoRepositorio ingresos;
    @Mock
    GastoRepositorio gastos;
    @Mock
    SucursalRepositorio sucursales;
    @Mock
    RutaRepositorio rutas;
    @Mock
    BusRepositorio buses;
    @Mock
    ChoferRepositorio choferes;
    @Mock
    ViajeRepositorio viajes;
    @Mock
    TarifaDepreciacionRepositorio tarifas;

    private final CompiladorJasper compilador = new CompiladorJasper();

    @BeforeEach
    void vacio() {
        when(sucursales.findAll()).thenReturn(List.of());
        when(sucursales.findAllByOrderByNombreAsc()).thenReturn(List.of());
        when(rutas.findAll()).thenReturn(List.of());
        when(buses.findAll()).thenReturn(List.of());
        when(choferes.findAll()).thenReturn(List.of());
        when(ingresos.buscar(any(), any(), any(), any())).thenReturn(List.of());
        when(ingresos.sumarPorSucursal(any(), any())).thenReturn(List.of());
        when(ingresos.demandaPorRuta(any(), any(), any(), any())).thenReturn(List.of());
        when(gastos.sumarPorSucursal(any(), any())).thenReturn(List.of());
        when(gastos.sumarPorSucursalYTipo(any(), any(), any())).thenReturn(List.of());
        when(gastos.tallerDetalleCategoria(any(), any(), any(), any())).thenReturn(List.of());
        when(gastos.tallerDetalleBus(any(), any(), any(), any(), any())).thenReturn(List.of());
        when(viajes.buscar(any(), any(), any(), any())).thenReturn(List.of());
        when(tarifas.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(any()))
                .thenReturn(Optional.empty());
        when(buses.findBySucursalIdOrderByPlacaAsc(any())).thenReturn(List.of());
        when(choferes.findBySucursalIdOrderByNombreCompletoAsc(any())).thenReturn(List.of());
    }

    private ParametrosReporte params() {
        return ParametrosReporte.builder().sucursalId(UUID.randomUUID()).limite(10).build();
    }

    private void esPdf(byte[] pdf) {
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void ingresos() throws Exception {
        esPdf(new GeneradorIngresosGlobales(ingresos, sucursales, compilador).generar(params()));
    }

    @Test
    void gastos() throws Exception {
        esPdf(new GeneradorGastosGlobales(gastos, sucursales, compilador).generar(params()));
    }

    @Test
    void ganancias() throws Exception {
        esPdf(new GeneradorGanancias(ingresos, gastos, sucursales, compilador).generar(params()));
    }

    @Test
    void rutasDemandadas() throws Exception {
        esPdf(new GeneradorRutasDemandadas(ingresos, rutas, compilador).generar(params()));
    }

    @Test
    void tallerCategoria() throws Exception {
        esPdf(new GeneradorTallerCategoria(gastos, buses, compilador).generar(params()));
    }

    @Test
    void tallerCategoriaConDatosAgrupaPorCategoria() throws Exception {
        UUID busId = UUID.randomUUID();
        when(gastos.tallerDetalleCategoria(any(), any(), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{UUID.randomUUID(), "Llantas", busId, java.time.LocalDate.of(2026, 3, 10),
                        "Cambio de llantas", new java.math.BigDecimal("400.00")},
                new Object[]{UUID.randomUUID(), "Frenos", busId, java.time.LocalDate.of(2026, 3, 11),
                        "Pastillas de freno", new java.math.BigDecimal("200.00")}));
        gt.cunoc.proyecto2.reporting.modelos.BusEntidad bus =
                new gt.cunoc.proyecto2.reporting.modelos.BusEntidad();
        bus.setId(busId);
        bus.setPlaca("C123ABC");
        when(buses.findAll()).thenReturn(List.of(bus));

        esPdf(new GeneradorTallerCategoria(gastos, buses, compilador).generar(params()));
    }

    @Test
    void ingresosAlquilerConFechas() throws Exception {
        var alquiler = new gt.cunoc.proyecto2.reporting.modelos.IngresoEntidad();
        alquiler.setSucursalId(UUID.randomUUID());
        alquiler.setTipo("ALQUILER");
        alquiler.setReferenciaId(UUID.randomUUID());
        alquiler.setClienteNit("1234567-8");
        alquiler.setClienteNombre("Colegio San José");
        alquiler.setOrigen("Quetzaltenango");
        alquiler.setDestino("Antigua Guatemala");
        alquiler.setFechaNegocio(java.time.LocalDate.of(2026, 11, 1));
        alquiler.setFechaSalida(java.time.OffsetDateTime.parse("2026-11-05T14:00:00Z"));
        alquiler.setFechaRetorno(java.time.OffsetDateTime.parse("2026-11-09T00:00:00Z"));
        alquiler.setMonto(new java.math.BigDecimal("2600.00"));
        when(ingresos.buscar(any(), any(), any(), any())).thenReturn(List.of(alquiler));

        esPdf(new GeneradorIngresosAlquiler(ingresos, compilador).generar(params()));
    }

    @Test
    void mapaRutas() throws Exception {
        esPdf(new GeneradorMapaRutas(sucursales, rutas, compilador).generar(params()));
    }

    @Test
    void buses() throws Exception {
        esPdf(new GeneradorBuses(buses, choferes, compilador).generar(params()));
    }

    @Test
    void choferes() throws Exception {
        esPdf(new GeneradorChoferes(choferes, compilador).generar(params()));
    }

    @Test
    void ingresosBoletos() throws Exception {
        esPdf(new GeneradorIngresosBoletos(ingresos, buses, compilador).generar(params()));
    }

    @Test
    void ingresosAlquiler() throws Exception {
        esPdf(new GeneradorIngresosAlquiler(ingresos, compilador).generar(params()));
    }

    @Test
    void depreciacion() throws Exception {
        esPdf(new GeneradorDepreciacion(viajes, buses, tarifas, compilador).generar(params()));
    }

    @Test
    void tallerBus() throws Exception {
        esPdf(new GeneradorTallerBus(gastos, buses, compilador).generar(params()));
    }

    @Test
    void tallerBusConDatosListaCadaGasto() throws Exception {
        UUID busId = UUID.randomUUID();
        when(gastos.tallerDetalleBus(any(), any(), any(), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{busId, UUID.randomUUID(), "Llantas", java.time.LocalDate.of(2026, 3, 10),
                        "Cambio de llantas", "REPUESTO", new java.math.BigDecimal("6800.00")},
                new Object[]{busId, UUID.randomUUID(), "Llantas", java.time.LocalDate.of(2026, 3, 10),
                        "Mano de obra", "MANO_OBRA", new java.math.BigDecimal("400.00")}));
        gt.cunoc.proyecto2.reporting.modelos.BusEntidad bus =
                new gt.cunoc.proyecto2.reporting.modelos.BusEntidad();
        bus.setId(busId);
        bus.setPlaca("C123ABC");
        when(buses.findAll()).thenReturn(List.of(bus));

        esPdf(new GeneradorTallerBus(gastos, buses, compilador).generar(params()));
    }
}
