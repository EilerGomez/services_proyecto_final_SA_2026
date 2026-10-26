package gt.cunoc.proyecto2.reporting.reportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorBuses;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorChoferes;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorDepreciacion;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorGanancias;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorGastosGlobales;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorIngresosBoletos;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorIngresosGlobales;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorRutasDemandadas;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorTallerBus;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorTallerCategoria;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorBuses;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorChoferes;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorDepreciacion;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorGanancias;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorGastosGlobales;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorIngresosBoletos;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorIngresosGlobales;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorRutasDemandadas;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorTallerBus;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorTallerCategoria;
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

/** Ramas borde: filtros con valor, nulos, sumas nulas y búsquedas sin resultado. */
@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class GeneradoresBordesTest {

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
    void gananciasFiltraSucursalYToleraSumaNula() {
        UUID norte = UUID.randomUUID();
        UUID sur = UUID.randomUUID();
        when(sucursales.findAllByOrderByNombreAsc())
                .thenReturn(List.of(sucursal(norte, "Norte"), sucursal(sur, "Sur")));
        when(ingresos.sumarPorSucursal(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{norte, null}));
        when(gastos.sumarPorSucursal(any(), any())).thenReturn(List.of());

        Map<String, Object> datos = (Map<String, Object>) new GeneradorGanancias(
                ingresos, gastos, sucursales, compilador).datos(
                ParametrosReporte.builder().sucursalId(norte).build());

        assertThat((List<?>) datos.get("filas")).hasSize(1);
        assertThat((Map<String, Object>) datos.get("totales"))
                .containsEntry("ganancia", BigDecimal.ZERO);
    }

    @Test
    void gastosFiltraSucursalYToleraSumaNula() {
        UUID norte = UUID.randomUUID();
        when(sucursales.findAllByOrderByNombreAsc()).thenReturn(List.of(sucursal(norte, "Norte")));
        when(gastos.sumarPorSucursalYTipo(any(), any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{norte, "COMBUSTIBLE", null}));

        Map<String, Object> datos = (Map<String, Object>) new GeneradorGastosGlobales(
                gastos, sucursales, compilador).datos(
                ParametrosReporte.builder().sucursalId(norte).build());

        assertThat(((Map<String, Object>) datos.get("totales")).get("combustible"))
                .isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void ingresosToleraSucursalDesconocida() throws Exception {
        IngresoEntidad i = new IngresoEntidad();
        i.setSucursalId(UUID.randomUUID());
        i.setTipo("BOLETO");
        i.setReferenciaId(UUID.randomUUID());
        i.setCantidadBoletos(1);
        i.setMonto(new BigDecimal("75"));
        i.setFechaNegocio(LocalDate.of(2026, 10, 20));
        when(sucursales.findAll()).thenReturn(List.of());
        when(sucursales.findAllByOrderByNombreAsc()).thenReturn(List.of());
        when(ingresos.buscar(any(), any(), any(), any())).thenReturn(List.of(i));

        var generador = new GeneradorIngresosGlobales(ingresos, sucursales, compilador);
        Map<String, Object> datos = (Map<String, Object>) generador.datos(
                ParametrosReporte.builder().build());

        // sin sucursales conocidas no hay grupos: el total queda en cero
        assertThat(datos.get("totalGlobal")).isEqualTo(BigDecimal.ZERO);
        // ...pero el PDF sí muestra la fila con sucursal "-"
        byte[] pdf = generador.generar(ParametrosReporte.builder().build());
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }

    @Test
    void boletosFiltranRutaYBusConPlacaDesconocida() {
        UUID rutaId = UUID.randomUUID();
        IngresoEntidad una = new IngresoEntidad();
        una.setSucursalId(UUID.randomUUID());
        una.setTipo("BOLETO");
        una.setReferenciaId(UUID.randomUUID());
        una.setRutaId(rutaId);
        una.setBusId(UUID.randomUUID());
        una.setCantidadBoletos(2);
        una.setMonto(new BigDecimal("150"));
        una.setFechaNegocio(LocalDate.of(2026, 10, 20));
        when(ingresos.buscar(any(), any(), any(), any())).thenReturn(List.of(una));
        when(buses.findAll()).thenReturn(List.of());

        var generador = new GeneradorIngresosBoletos(ingresos, buses, compilador);
        Map<String, Object> todo = (Map<String, Object>) generador.datos(
                ParametrosReporte.builder().build());
        assertThat(((List<Map<String, Object>>) todo.get("filas")).get(0).get("busPlaca"))
                .isEqualTo("-");

        Map<String, Object> filtrado = (Map<String, Object>) generador.datos(
                ParametrosReporte.builder().rutaId(UUID.randomUUID()).busId(UUID.randomUUID()).build());
        assertThat((List<?>) filtrado.get("filas")).isEmpty();
    }

    @Test
    void busesConEstadoYChofer() {
        UUID sucursalId = UUID.randomUUID();
        BusEntidad conChofer = new BusEntidad();
        conChofer.setId(UUID.randomUUID());
        conChofer.setPlaca("C1");
        conChofer.setChoferActualId(UUID.randomUUID());
        BusEntidad sinChofer = new BusEntidad();
        sinChofer.setId(UUID.randomUUID());
        sinChofer.setPlaca("C2");
        sinChofer.setEstado("EN_TALLER");
        when(buses.findBySucursalIdOrderByPlacaAsc(sucursalId))
                .thenReturn(List.of(conChofer, sinChofer));
        when(choferes.findAll()).thenReturn(List.of());

        var generador = new GeneradorBuses(buses, choferes, compilador);
        List<Map<String, Object>> todos = (List<Map<String, Object>>) generador.datos(
                ParametrosReporte.builder().sucursalId(sucursalId).build());
        assertThat(todos).hasSize(2);
        assertThat(todos.get(1).get("choferActual")).isEqualTo("-");

        List<Map<String, Object>> activos = (List<Map<String, Object>>) generador.datos(
                ParametrosReporte.builder().sucursalId(sucursalId).estado("ACTIVO").build());
        assertThat(activos).isEmpty();
    }

    @Test
    void choferesConEstadoYSinVencimiento() {
        UUID sucursalId = UUID.randomUUID();
        ChoferEntidad chofer = new ChoferEntidad();
        chofer.setId(UUID.randomUUID());
        chofer.setNombreCompleto("X");
        chofer.setEstado("INACTIVO");
        when(choferes.findBySucursalIdOrderByNombreCompletoAsc(sucursalId))
                .thenReturn(List.of(chofer));

        var generador = new GeneradorChoferes(choferes, compilador);
        List<Map<String, Object>> todos = (List<Map<String, Object>>) generador.datos(
                ParametrosReporte.builder().sucursalId(sucursalId).build());
        assertThat(todos.get(0).get("fechaVencimientoLicencia")).isEqualTo("-");

        List<Map<String, Object>> activos = (List<Map<String, Object>>) generador.datos(
                ParametrosReporte.builder().sucursalId(sucursalId).estado("ACTIVO").build());
        assertThat(activos).isEmpty();
    }

    @Test
    void rutasDemandadasConLimiteYNulos() {
        when(rutas.findAll()).thenReturn(List.of());
        when(ingresos.demandaPorRuta(any(), any(), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{UUID.randomUUID(), null, null, null, null}));

        List<Map<String, Object>> datos = (List<Map<String, Object>>) new GeneradorRutasDemandadas(
                ingresos, rutas, compilador).datos(
                ParametrosReporte.builder().limite(5).build());

        assertThat(datos).hasSize(1);
        assertThat(datos.get(0).get("boletos")).isEqualTo(0L);
        assertThat(datos.get(0).get("origen")).isEqualTo("-");
    }

    @Test
    void tallerCategoriaConNulos() {
        when(gastos.tallerDetalleCategoria(any(), any(), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{null, null, null, null, null, null}));
        when(buses.findAll()).thenReturn(List.of());

        Map<String, Object> datos = (Map<String, Object>) new GeneradorTallerCategoria(
                gastos, buses, compilador).datos(ParametrosReporte.builder().build());

        List<Map<String, Object>> filas = (List<Map<String, Object>>) datos.get("filas");
        assertThat(filas.get(0).get("categoria")).isEqualTo("-");
        assertThat(filas.get(0).get("placa")).isEqualTo("-");
        assertThat(filas.get(0).get("fecha")).isEqualTo("-");
        assertThat(datos.get("total")).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void tallerBusConBusDesconocidoYNulos() {
        when(gastos.tallerDetalleBus(any(), any(), any(), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{UUID.randomUUID(), UUID.randomUUID(), null, null, null, null, null}));
        when(buses.findAll()).thenReturn(List.of());

        Map<String, Object> datos = (Map<String, Object>) new GeneradorTallerBus(
                gastos, buses, compilador).datos(
                ParametrosReporte.builder().sucursalId(UUID.randomUUID()).build());

        assertThat(((List<Map<String, Object>>) datos.get("buses")).get(0).get("placa"))
                .isEqualTo("-");
        assertThat(datos.get("total")).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    void depreciacionConNulosTarifaYFechas() {
        UUID busId = UUID.randomUUID();
        ViajeEntidad viaje = new ViajeEntidad();
        viaje.setViajeId(UUID.randomUUID());
        viaje.setBusId(busId);
        viaje.setEstado("COMPLETADO");
        when(viajes.buscar(any(), any(), any(), any())).thenReturn(List.of(viaje));
        when(buses.findAll()).thenReturn(List.of());
        gt.cunoc.proyecto2.reporting.modelos.TarifaDepreciacionEntidad tarifa =
                new gt.cunoc.proyecto2.reporting.modelos.TarifaDepreciacionEntidad();
        tarifa.setMontoPorKm(new BigDecimal("1.25"));
        tarifa.setVigenteDesde(LocalDate.of(2026, 1, 1));
        when(tarifas.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(any()))
                .thenReturn(Optional.of(tarifa));

        Map<String, Object> datos = (Map<String, Object>) new GeneradorDepreciacion(
                viajes, buses, tarifas, compilador).datos(ParametrosReporte.builder()
                .desde(LocalDate.of(2026, 10, 1)).hasta(LocalDate.of(2026, 10, 31)).build());

        assertThat(((Map<String, Object>) datos.get("tarifaVigente")).get("montoPorKm"))
                .isEqualTo(new BigDecimal("1.25"));
        assertThat(datos.get("total")).isEqualTo(BigDecimal.ZERO);
    }
}
