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
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorIngresosAlquiler;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorIngresosBoletos;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorIngresosGlobales;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorGastosGlobales;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorMapaRutas;
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

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class GeneradoresDatosTest {

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

    private IngresoEntidad ingreso(UUID sucursalId, String tipo, String monto, int boletos) {
        IngresoEntidad i = new IngresoEntidad();
        i.setSucursalId(sucursalId);
        i.setTipo(tipo);
        i.setReferenciaId(UUID.randomUUID());
        i.setClienteNit("CF");
        i.setClienteNombre("CF");
        i.setOrigen("Xela");
        i.setDestino("Guate");
        i.setCantidadBoletos(boletos);
        i.setMonto(new BigDecimal(monto));
        i.setFechaNegocio(LocalDate.of(2026, 10, 20));
        return i;
    }

    @Test
    void ingresosGlobalesAgrupaConSubtotalYTotal() {
        UUID norte = UUID.randomUUID();
        when(sucursales.findAll()).thenReturn(List.of(sucursal(norte, "Norte")));
        when(sucursales.findAllByOrderByNombreAsc()).thenReturn(List.of(sucursal(norte, "Norte")));
        IngresoEntidad cancelacion = ingreso(norte, "BOLETO", "-150.00", -2);
        when(ingresos.buscar(any(), any(), any(), any())).thenReturn(
                List.of(ingreso(norte, "BOLETO", "150.00", 2), ingreso(norte, "ALQUILER", "4300.00", 0),
                        cancelacion));

        Map<String, Object> datos = (Map<String, Object>) new GeneradorIngresosGlobales(
                ingresos, sucursales, compilador).datos(ParametrosReporte.builder().build());

        assertThat((List<Map<String, Object>>) datos.get("sucursales")).hasSize(1);
        assertThat(datos.get("totalGlobal")).isEqualTo(new BigDecimal("4300.00"));
        List<Map<String, Object>> filas =
                (List<Map<String, Object>>) ((List<Map<String, Object>>) datos.get("sucursales")).get(0).get("filas");
        assertThat(filas.stream().map(f -> f.get("detalle")).toList())
                .contains("Cancelación", "Xela → Guate");
    }

    @Test
    void ingresosGlobalesRespetaFiltroSucursal() {
        UUID norte = UUID.randomUUID();
        UUID sur = UUID.randomUUID();
        when(sucursales.findAll()).thenReturn(List.of(sucursal(norte, "Norte")));
        when(sucursales.findAllByOrderByNombreAsc()).thenReturn(
                List.of(sucursal(norte, "Norte"), sucursal(sur, "Sur")));
        when(ingresos.buscar(any(), any(), any(), any())).thenReturn(
                List.of(ingreso(norte, "BOLETO", "150.00", 2)));

        Map<String, Object> datos = (Map<String, Object>) new GeneradorIngresosGlobales(
                ingresos, sucursales, compilador).datos(
                ParametrosReporte.builder().sucursalId(norte).build());

        assertThat((List<Map<String, Object>>) datos.get("sucursales")).hasSize(1);
        assertThat(datos.get("totalGlobal")).isEqualTo(new BigDecimal("150.00"));
    }

    @Test
    void gastosGlobalesMapeaTipos() {
        UUID norte = UUID.randomUUID();
        when(sucursales.findAllByOrderByNombreAsc()).thenReturn(List.of(sucursal(norte, "Norte")));
        when(gastos.sumarPorSucursalYTipo(any(), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{norte, "COMBUSTIBLE", new BigDecimal("950.00")},
                new Object[]{norte, "MANO_OBRA", new BigDecimal("750.00")},
                new Object[]{norte, "SALARIO_CHOFER", new BigDecimal("350.00")}));

        Map<String, Object> datos = (Map<String, Object>) new GeneradorGastosGlobales(
                gastos, sucursales, compilador).datos(ParametrosReporte.builder().build());

        Map<String, Object> totales = (Map<String, Object>) datos.get("totales");
        assertThat(totales.get("combustible")).isEqualTo(new BigDecimal("950.00"));
        assertThat(totales.get("taller")).isEqualTo(new BigDecimal("750.00"));
        assertThat(totales.get("repuestos")).isEqualTo(BigDecimal.ZERO);
        assertThat(totales.get("total")).isEqualTo(new BigDecimal("2050.00"));
    }

    @Test
    void rutasDemandadasOrdenaPorBoletos() {
        UUID rutaId = UUID.randomUUID();
        RutaEntidad ruta = new RutaEntidad();
        ruta.setId(rutaId);
        ruta.setOrigenNombre("Xela");
        ruta.setDestinoNombre("Guate");
        when(rutas.findAll()).thenReturn(List.of(ruta));
        when(ingresos.demandaPorRuta(any(), any(), any())).thenReturn(
                List.<Object[]>of(new Object[]{rutaId, 10L, new BigDecimal("750.00")}));

        List<Map<String, Object>> datos = (List<Map<String, Object>>) new GeneradorRutasDemandadas(
                ingresos, rutas, compilador).datos(ParametrosReporte.builder().build());

        assertThat(datos).hasSize(1);
        assertThat(datos.get(0).get("boletos")).isEqualTo(10L);
        assertThat(datos.get(0).get("origen")).isEqualTo("Xela");
    }

    @Test
    void tallerCategoriaSumaManoObraYRepuestos() {
        when(gastos.tallerPorCategoria(any(), any(), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{UUID.randomUUID(), "Llantas", new BigDecimal("400.00"),
                        new BigDecimal("6800.00"), new BigDecimal("7200.00")}));

        Map<String, Object> datos = (Map<String, Object>) new GeneradorTallerCategoria(
                gastos, compilador).datos(ParametrosReporte.builder().build());

        assertThat(((List<?>) datos.get("filas"))).hasSize(1);
        assertThat(datos.get("total")).isEqualTo(new BigDecimal("7200.00"));
    }

    @Test
    void mapaRutasDevuelveSucursalesYRutas() {
        UUID norte = UUID.randomUUID();
        UUID sur = UUID.randomUUID();
        when(sucursales.findAllByOrderByNombreAsc()).thenReturn(
                List.of(sucursal(norte, "Norte"), sucursal(sur, "Sur")));
        when(sucursales.findAll()).thenReturn(
                List.of(sucursal(norte, "Norte"), sucursal(sur, "Sur")));
        RutaEntidad ruta = new RutaEntidad();
        ruta.setId(UUID.randomUUID());
        ruta.setSucursalOrigenId(norte);
        ruta.setSucursalDestinoId(sur);
        ruta.setOrigenNombre("Norte");
        ruta.setDestinoNombre("Sur");
        ruta.setDistanciaKm(new BigDecimal("205"));
        ruta.setPrecioBoleto(new BigDecimal("75"));
        ruta.setActiva(true);
        when(rutas.findAll()).thenReturn(List.of(ruta));

        Map<String, Object> todo = (Map<String, Object>) new GeneradorMapaRutas(
                sucursales, rutas, compilador).datos(ParametrosReporte.builder().build());
        assertThat((List<?>) todo.get("sucursales")).hasSize(2);
        assertThat((List<?>) todo.get("rutas")).hasSize(1);

        Map<String, Object> filtrado = (Map<String, Object>) new GeneradorMapaRutas(
                sucursales, rutas, compilador).datos(
                ParametrosReporte.builder().sucursalId(sur).build());
        assertThat((List<?>) filtrado.get("rutas")).isEmpty();
    }

    @Test
    void busesResuelvenChoferActualYFiltranEstado() {
        UUID sucursalId = UUID.randomUUID();
        UUID choferId = UUID.randomUUID();
        BusEntidad bus = new BusEntidad();
        bus.setId(UUID.randomUUID());
        bus.setSucursalId(sucursalId);
        bus.setPlaca("C123ABC");
        bus.setMarca("Volvo");
        bus.setModelo("B12");
        bus.setCapacidadPasajeros(50);
        bus.setEstado("ACTIVO");
        bus.setChoferActualId(choferId);
        bus.setKilometrajeActual(new BigDecimal("1000"));
        when(buses.findBySucursalIdOrderByPlacaAsc(sucursalId)).thenReturn(List.of(bus));
        ChoferEntidad chofer = new ChoferEntidad();
        chofer.setId(choferId);
        chofer.setNombreCompleto("Juan Pérez");
        when(choferes.findAll()).thenReturn(List.of(chofer));

        List<Map<String, Object>> datos = (List<Map<String, Object>>) new GeneradorBuses(
                buses, choferes, compilador).datos(
                ParametrosReporte.builder().sucursalId(sucursalId).build());

        assertThat(datos).hasSize(1);
        assertThat(datos.get(0).get("choferActual")).isEqualTo("Juan Pérez");

        List<Map<String, Object>> filtrados = (List<Map<String, Object>>) new GeneradorBuses(
                buses, choferes, compilador).datos(
                ParametrosReporte.builder().sucursalId(sucursalId).estado("EN_TALLER").build());
        assertThat(filtrados).isEmpty();
    }

    @Test
    void choferesMapeanVencimiento() {
        UUID sucursalId = UUID.randomUUID();
        ChoferEntidad chofer = new ChoferEntidad();
        chofer.setId(UUID.randomUUID());
        chofer.setNombreCompleto("Ana López");
        chofer.setNumeroLicencia("LIC-2");
        chofer.setTipoLicencia("B");
        chofer.setFechaVencimientoLicencia(LocalDate.of(2027, 3, 15));
        chofer.setEstado("ACTIVO");
        chofer.setTotalViajes(3);
        when(choferes.findBySucursalIdOrderByNombreCompletoAsc(sucursalId))
                .thenReturn(List.of(chofer));

        List<Map<String, Object>> datos = (List<Map<String, Object>>) new GeneradorChoferes(
                choferes, compilador).datos(
                ParametrosReporte.builder().sucursalId(sucursalId).build());

        assertThat(datos).hasSize(1);
        assertThat(datos.get(0).get("fechaVencimientoLicencia")).isEqualTo("2027-03-15");
        assertThat(datos.get(0).get("totalViajes")).isEqualTo(3);
    }

    @Test
    void ingresosBoletosFiltranRutaYBusYSuman() {
        UUID sucursalId = UUID.randomUUID();
        UUID rutaId = UUID.randomUUID();
        UUID busId = UUID.randomUUID();
        IngresoEntidad uno = ingreso(sucursalId, "BOLETO", "150.00", 2);
        uno.setRutaId(rutaId);
        uno.setBusId(busId);
        IngresoEntidad dos = ingreso(sucursalId, "BOLETO", "75.00", 1);
        dos.setRutaId(UUID.randomUUID());
        dos.setBusId(busId);
        when(ingresos.buscar(any(), any(), any(), any())).thenReturn(List.of(uno, dos));
        BusEntidad bus = new BusEntidad();
        bus.setId(busId);
        bus.setPlaca("C123ABC");
        when(buses.findAll()).thenReturn(List.of(bus));

        Map<String, Object> datos = (Map<String, Object>) new GeneradorIngresosBoletos(
                ingresos, buses, compilador).datos(ParametrosReporte.builder()
                .sucursalId(sucursalId).rutaId(rutaId).build());

        assertThat((List<?>) datos.get("filas")).hasSize(1);
        assertThat(datos.get("totalBoletos")).isEqualTo(2);
        assertThat(datos.get("total")).isEqualTo(new BigDecimal("150.00"));
    }

    @Test
    void ingresosAlquilerMapeanOrigenDestino() {
        UUID sucursalId = UUID.randomUUID();
        IngresoEntidad alq = ingreso(sucursalId, "ALQUILER", "4300.00", 0);
        when(ingresos.buscar(any(), any(), any(), any())).thenReturn(List.of(alq));

        Map<String, Object> datos = (Map<String, Object>) new GeneradorIngresosAlquiler(
                ingresos, compilador).datos(ParametrosReporte.builder().sucursalId(sucursalId).build());

        assertThat(((List<Map<String, Object>>) datos.get("filas")).get(0).get("origen"))
                .isEqualTo("Xela");
        assertThat(datos.get("total")).isEqualTo(new BigDecimal("4300.00"));
    }

    @Test
    void depreciacionAgrupaViajesCompletadosPorBus() {
        UUID sucursalId = UUID.randomUUID();
        UUID busId = UUID.randomUUID();
        ViajeEntidad completo = new ViajeEntidad();
        completo.setViajeId(UUID.randomUUID());
        completo.setBusId(busId);
        completo.setFechaSalida(OffsetDateTime.of(2026, 10, 20, 8, 0, 0, 0, ZoneOffset.UTC));
        completo.setEstado("COMPLETADO");
        completo.setKmRecorridos(new BigDecimal("50"));
        completo.setTarifaDepreciacion(new BigDecimal("1.5"));
        completo.setCostoDepreciacion(new BigDecimal("75"));
        ViajeEntidad programado = new ViajeEntidad();
        programado.setViajeId(UUID.randomUUID());
        programado.setBusId(busId);
        programado.setEstado("PROGRAMADO");
        when(viajes.buscar(any(), any(), any(), any()))
                .thenReturn(List.of(completo, programado));
        BusEntidad bus = new BusEntidad();
        bus.setId(busId);
        bus.setPlaca("C123ABC");
        when(buses.findAll()).thenReturn(List.of(bus));
        when(tarifas.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(any()))
                .thenReturn(Optional.empty());

        Map<String, Object> datos = (Map<String, Object>) new GeneradorDepreciacion(
                viajes, buses, tarifas, compilador).datos(
                ParametrosReporte.builder().sucursalId(sucursalId).build());

        assertThat(datos.get("tarifaVigente")).isNull();
        assertThat(((List<Map<String, Object>>) datos.get("buses")).get(0)
                .get("depreciacionAcumulada")).isEqualTo(new BigDecimal("75"));
        assertThat(datos.get("total")).isEqualTo(new BigDecimal("75"));
    }

    @Test
    void tallerBusAgrupaPorBusYCategoria() {
        UUID busId = UUID.randomUUID();
        when(gastos.tallerPorBusYCategoria(any(), any(), any(), any())).thenReturn(List.<Object[]>of(
                new Object[]{busId, UUID.randomUUID(), "Llantas", new BigDecimal("400.00"),
                        new BigDecimal("6800.00"), new BigDecimal("7200.00")}));
        BusEntidad bus = new BusEntidad();
        bus.setId(busId);
        bus.setPlaca("C123ABC");
        when(buses.findAll()).thenReturn(List.of(bus));

        Map<String, Object> datos = (Map<String, Object>) new GeneradorTallerBus(
                gastos, buses, compilador).datos(
                ParametrosReporte.builder().sucursalId(UUID.randomUUID()).build());

        assertThat(((List<?>) datos.get("buses"))).hasSize(1);
        assertThat(datos.get("total")).isEqualTo(new BigDecimal("7200.00"));
    }
}
