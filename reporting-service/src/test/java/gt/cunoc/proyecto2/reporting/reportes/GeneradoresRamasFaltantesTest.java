package gt.cunoc.proyecto2.reporting.reportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.reporting.modelos.BusEntidad;
import gt.cunoc.proyecto2.reporting.modelos.ChoferEntidad;
import gt.cunoc.proyecto2.reporting.modelos.IngresoEntidad;
import gt.cunoc.proyecto2.reporting.modelos.SucursalEntidad;
import gt.cunoc.proyecto2.reporting.modelos.TarifaDepreciacionEntidad;
import gt.cunoc.proyecto2.reporting.modelos.ViajeEntidad;
import gt.cunoc.proyecto2.reporting.repositorios.BusRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.ChoferRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.GastoRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.IngresoRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.SucursalRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.TarifaDepreciacionRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.ViajeRepositorio;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorBuses;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorChoferes;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorDepreciacion;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorGastosGlobales;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorIngresosBoletos;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Ramas que quedaban sin cubrir: filtros coincidentes, nulos y rutas de error. */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class GeneradoresRamasFaltantesTest {

    @Mock
    IngresoRepositorio ingresos;
    @Mock
    GastoRepositorio gastos;
    @Mock
    SucursalRepositorio sucursales;
    @Mock
    BusRepositorio buses;
    @Mock
    ChoferRepositorio choferes;
    @Mock
    ViajeRepositorio viajes;
    @Mock
    TarifaDepreciacionRepositorio tarifas;

    private final CompiladorJasper compilador = new CompiladorJasper();

    private SucursalEntidad sucursal(UUID id, String nombre) {
        SucursalEntidad s = new SucursalEntidad();
        s.setId(id);
        s.setNombre(nombre);
        s.setLatitud(BigDecimal.ONE);
        s.setLongitud(BigDecimal.ONE);
        s.setActiva(true);
        return s;
    }

    @Test
    void tiposReportanSuTipo() {
        assertThat(new GeneradorBuses(buses, choferes, compilador).tipo())
                .isEqualTo(TipoReporte.BUSES);
        assertThat(new GeneradorChoferes(choferes, compilador).tipo())
                .isEqualTo(TipoReporte.CHOFERES);
        assertThat(new GeneradorIngresosBoletos(ingresos, buses, compilador).tipo())
                .isEqualTo(TipoReporte.INGRESOS_BOLETOS);
        assertThat(new GeneradorGastosGlobales(gastos, sucursales, compilador).tipo())
                .isEqualTo(TipoReporte.GASTOS);
        assertThat(new GeneradorDepreciacion(viajes, buses, tarifas, compilador).tipo())
                .isEqualTo(TipoReporte.DEPRECIACION);
    }

    @Test
    void busesFiltraPorEstadoCoincidente() {
        UUID sucursalId = UUID.randomUUID();
        BusEntidad bus = new BusEntidad();
        bus.setId(UUID.randomUUID());
        bus.setPlaca("C123ABC");
        bus.setEstado("ACTIVO");
        when(buses.findBySucursalIdOrderByPlacaAsc(sucursalId)).thenReturn(List.of(bus));
        when(choferes.findAll()).thenReturn(List.of());

        List<Map<String, Object>> datos = (List<Map<String, Object>>) new GeneradorBuses(
                buses, choferes, compilador).datos(
                ParametrosReporte.builder().sucursalId(sucursalId).estado("ACTIVO").build());

        assertThat(datos).hasSize(1);
        assertThat(datos.get(0).get("placa")).isEqualTo("C123ABC");
    }

    @Test
    void choferesFiltraPorEstadoCoincidente() {
        UUID sucursalId = UUID.randomUUID();
        ChoferEntidad chofer = new ChoferEntidad();
        chofer.setId(UUID.randomUUID());
        chofer.setNombreCompleto("Ana López");
        chofer.setEstado("ACTIVO");
        when(choferes.findBySucursalIdOrderByNombreCompletoAsc(sucursalId))
                .thenReturn(List.of(chofer));

        List<Map<String, Object>> datos = (List<Map<String, Object>>) new GeneradorChoferes(
                choferes, compilador).datos(
                ParametrosReporte.builder().sucursalId(sucursalId).estado("ACTIVO").build());

        assertThat(datos).hasSize(1);
        assertThat(datos.get(0).get("nombreCompleto")).isEqualTo("Ana López");
    }

    @Test
    void boletosFiltraPorBusCoincidente() {
        UUID busId = UUID.randomUUID();
        IngresoEntidad uno = new IngresoEntidad();
        uno.setSucursalId(UUID.randomUUID());
        uno.setTipo("BOLETO");
        uno.setReferenciaId(UUID.randomUUID());
        uno.setBusId(busId);
        uno.setCantidadBoletos(2);
        uno.setMonto(new BigDecimal("150.00"));
        uno.setFechaNegocio(LocalDate.of(2026, 10, 20));
        IngresoEntidad dos = new IngresoEntidad();
        dos.setSucursalId(UUID.randomUUID());
        dos.setTipo("BOLETO");
        dos.setReferenciaId(UUID.randomUUID());
        dos.setBusId(UUID.randomUUID());
        dos.setCantidadBoletos(1);
        dos.setMonto(new BigDecimal("75.00"));
        dos.setFechaNegocio(LocalDate.of(2026, 10, 21));
        when(ingresos.buscar(any(), any(), any(), any())).thenReturn(List.of(uno, dos));
        BusEntidad bus = new BusEntidad();
        bus.setId(busId);
        bus.setPlaca("C123ABC");
        when(buses.findAll()).thenReturn(List.of(bus));

        Map<String, Object> datos = (Map<String, Object>) new GeneradorIngresosBoletos(
                ingresos, buses, compilador).datos(
                ParametrosReporte.builder().busId(busId).build());

        assertThat((List<?>) datos.get("filas")).hasSize(1);
        assertThat(((List<Map<String, Object>>) datos.get("filas")).get(0).get("busPlaca"))
                .isEqualTo("C123ABC");
        assertThat(datos.get("total")).isEqualTo(new BigDecimal("150.00"));
    }

    @Test
    void boletosToleraBusNulo() {
        IngresoEntidad ingreso = new IngresoEntidad();
        ingreso.setSucursalId(UUID.randomUUID());
        ingreso.setTipo("BOLETO");
        ingreso.setReferenciaId(UUID.randomUUID());
        ingreso.setBusId(null);
        ingreso.setCantidadBoletos(1);
        ingreso.setMonto(new BigDecimal("75.00"));
        ingreso.setFechaNegocio(LocalDate.of(2026, 10, 20));
        when(ingresos.buscar(any(), any(), any(), any())).thenReturn(List.of(ingreso));
        when(buses.findAll()).thenReturn(List.of());

        Map<String, Object> datos = (Map<String, Object>) new GeneradorIngresosBoletos(
                ingresos, buses, compilador).datos(ParametrosReporte.builder().build());

        assertThat(((List<Map<String, Object>>) datos.get("filas")).get(0).get("busPlaca"))
                .isEqualTo("-");
    }

    @Test
    void gastosOmiteSucursalDistintaAlFiltro() {
        UUID norte = UUID.randomUUID();
        UUID sur = UUID.randomUUID();
        when(sucursales.findAllByOrderByNombreAsc())
                .thenReturn(List.of(sucursal(norte, "Norte"), sucursal(sur, "Sur")));
        when(gastos.sumarPorSucursalYTipo(any(), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{norte, "COMBUSTIBLE", new BigDecimal("100.00")},
                new Object[]{sur, "COMBUSTIBLE", new BigDecimal("50.00")}));

        Map<String, Object> datos = (Map<String, Object>) new GeneradorGastosGlobales(
                gastos, sucursales, compilador).datos(
                ParametrosReporte.builder().sucursalId(norte).build());

        List<Map<String, Object>> filas = (List<Map<String, Object>>) datos.get("porSucursal");
        assertThat(filas).hasSize(1);
        assertThat(filas.get(0).get("sucursal")).isEqualTo("Norte");
        assertThat(((Map<String, Object>) datos.get("totales")).get("total"))
                .isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    void gastosGenerarToleraMontoNulo() throws Exception {
        UUID norte = UUID.randomUUID();
        when(sucursales.findAll()).thenReturn(List.of(sucursal(norte, "Norte")));
        when(gastos.sumarPorSucursalYTipo(any(), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{norte, "COMBUSTIBLE", new BigDecimal("100.00")},
                new Object[]{norte, "REPUESTO", null}));

        byte[] pdf = new GeneradorGastosGlobales(gastos, sucursales, compilador)
                .generar(ParametrosReporte.builder().build());

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void depreciacionGenerarFiltraCostoNuloYMuestraTarifa() throws Exception {
        UUID busId = UUID.randomUUID();
        ViajeEntidad sinCosto = new ViajeEntidad();
        sinCosto.setViajeId(UUID.randomUUID());
        sinCosto.setBusId(busId);
        sinCosto.setEstado("COMPLETADO");
        sinCosto.setFechaSalida(OffsetDateTime.of(2026, 10, 20, 8, 0, 0, 0, ZoneOffset.UTC));
        sinCosto.setKmRecorridos(new BigDecimal("50"));
        ViajeEntidad conCosto = new ViajeEntidad();
        conCosto.setViajeId(UUID.randomUUID());
        conCosto.setBusId(busId);
        conCosto.setEstado("COMPLETADO");
        conCosto.setFechaSalida(OffsetDateTime.of(2026, 10, 21, 8, 0, 0, 0, ZoneOffset.UTC));
        conCosto.setKmRecorridos(new BigDecimal("50"));
        conCosto.setTarifaDepreciacion(new BigDecimal("1.5"));
        conCosto.setCostoDepreciacion(new BigDecimal("75"));
        when(viajes.buscar(any(), any(), any(), any()))
                .thenReturn(List.of(sinCosto, conCosto));
        BusEntidad bus = new BusEntidad();
        bus.setId(busId);
        bus.setPlaca("C123ABC");
        when(buses.findAll()).thenReturn(List.of(bus));
        TarifaDepreciacionEntidad tarifa = new TarifaDepreciacionEntidad();
        tarifa.setMontoPorKm(new BigDecimal("1.5"));
        tarifa.setVigenteDesde(LocalDate.of(2026, 1, 1));
        when(tarifas.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(any()))
                .thenReturn(Optional.of(tarifa));

        byte[] pdf = new GeneradorDepreciacion(viajes, buses, tarifas, compilador)
                .generar(ParametrosReporte.builder()
                        .desde(LocalDate.of(2026, 10, 1)).hasta(LocalDate.of(2026, 10, 31)).build());

        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void depreciacionTarifaSinFecha() {
        UUID busId = UUID.randomUUID();
        ViajeEntidad viaje = new ViajeEntidad();
        viaje.setViajeId(UUID.randomUUID());
        viaje.setBusId(busId);
        viaje.setEstado("COMPLETADO");
        when(viajes.buscar(any(), any(), any(), any())).thenReturn(List.of(viaje));
        when(buses.findAll()).thenReturn(List.of());
        TarifaDepreciacionEntidad tarifa = new TarifaDepreciacionEntidad();
        tarifa.setMontoPorKm(new BigDecimal("1.5"));
        tarifa.setVigenteDesde(null);
        when(tarifas.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(any()))
                .thenReturn(Optional.of(tarifa));

        Map<String, Object> datos = (Map<String, Object>) new GeneradorDepreciacion(
                viajes, buses, tarifas, compilador).datos(ParametrosReporte.builder().build());

        assertThat(((Map<String, Object>) datos.get("tarifaVigente")).get("vigenteDesde"))
                .isNull();
    }

    @Test
    void compiladorErrorCompilacionEnvuelveJRException() {
        assertThatThrownBy(() -> compilador.obtener("roto"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Error compilando reporte");
    }
}
