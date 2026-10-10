package gt.cunoc.proyecto2.reporting.reportes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import gt.cunoc.proyecto2.reporting.modelos.SucursalEntidad;
import gt.cunoc.proyecto2.reporting.repositorios.GastoRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.IngresoRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.SucursalRepositorio;
import gt.cunoc.proyecto2.reporting.reportes.generadores.GeneradorGanancias;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GeneradorGananciasTest {

    @Mock
    IngresoRepositorio ingresos;
    @Mock
    GastoRepositorio gastos;
    @Mock
    SucursalRepositorio sucursales;

    GeneradorGanancias generador;

    @BeforeEach
    void armar() {
        generador = new GeneradorGanancias(ingresos, gastos, sucursales, new CompiladorJasper());
    }

    @Test
    @SuppressWarnings("unchecked")
    void datosAgrupaPorSucursalYCalculaGanancia() {
        UUID norte = UUID.randomUUID();
        SucursalEntidad sucursal = new SucursalEntidad();
        sucursal.setId(norte);
        sucursal.setNombre("Norte");
        when(sucursales.findAllByOrderByNombreAsc()).thenReturn(List.of(sucursal));
        when(ingresos.sumarPorSucursal(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{norte, new BigDecimal("1000.00")}));
        when(gastos.sumarPorSucursal(any(), any()))
                .thenReturn(List.<Object[]>of(new Object[]{norte, new BigDecimal("400.00")}));

        Map<String, Object> datos = (Map<String, Object>) generador.datos(
                ParametrosReporte.builder().build());

        assertThat((Map<String, Object>) datos.get("totales")).containsEntry("ingresos", new BigDecimal("1000.00"));
        assertThat((Map<String, Object>) datos.get("totales")).containsEntry("gastos", new BigDecimal("400.00"));
        assertThat((Map<String, Object>) datos.get("totales")).containsEntry("ganancia", new BigDecimal("600.00"));
    }

    @Test
    void generarDevuelvePdfValido() throws Exception {
        when(sucursales.findAllByOrderByNombreAsc()).thenReturn(List.of());
        when(ingresos.sumarPorSucursal(any(), any())).thenReturn(List.of());
        when(gastos.sumarPorSucursal(any(), any())).thenReturn(List.of());

        byte[] pdf = generador.generar(ParametrosReporte.builder().build());

        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 5)).isEqualTo("%PDF-");
    }
}
