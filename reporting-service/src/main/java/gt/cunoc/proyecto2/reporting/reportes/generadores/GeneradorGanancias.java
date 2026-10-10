package gt.cunoc.proyecto2.reporting.reportes.generadores;

import gt.cunoc.proyecto2.reporting.repositorios.GastoRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.IngresoRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.SucursalRepositorio;
import gt.cunoc.proyecto2.reporting.reportes.CompiladorJasper;
import gt.cunoc.proyecto2.reporting.reportes.GeneradorReporte;
import gt.cunoc.proyecto2.reporting.reportes.ParametrosReporte;
import gt.cunoc.proyecto2.reporting.reportes.TipoReporte;
import gt.cunoc.proyecto2.reporting.reportes.filas.GananciaFila;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Component;

/** RG3 · Ganancias por sucursal, en orden alfabético. */
@Component
public class GeneradorGanancias implements GeneradorReporte {

    private final IngresoRepositorio ingresos;
    private final GastoRepositorio gastos;
    private final SucursalRepositorio sucursales;
    private final CompiladorJasper compilador;

    public GeneradorGanancias(IngresoRepositorio ingresos, GastoRepositorio gastos,
                              SucursalRepositorio sucursales, CompiladorJasper compilador) {
        this.ingresos = ingresos;
        this.gastos = gastos;
        this.sucursales = sucursales;
        this.compilador = compilador;
    }

    @Override
    public TipoReporte tipo() {
        return TipoReporte.GANANCIAS;
    }

    @Override
    public byte[] generar(ParametrosReporte params) throws Exception {
        List<GananciaFila> filas = filas(params);
        BigDecimal totalIngresos = suma(filas, GananciaFila::getIngresos);
        BigDecimal totalGastos = suma(filas, GananciaFila::getGastos);

        JasperReport reporte = compilador.obtener("ganancias");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("REPORT_TITLE", "Reporte de Ganancias por Sucursal");
        parametros.put("FROM_DATE", Formatos.texto(params.getDesde()));
        parametros.put("TO_DATE", Formatos.texto(params.getHasta()));
        parametros.put("TOTAL_INGRESOS", Formatos.quetzales(totalIngresos));
        parametros.put("TOTAL_GASTOS", Formatos.quetzales(totalGastos));
        parametros.put("TOTAL_GANANCIA", Formatos.quetzales(totalIngresos.subtract(totalGastos)));

        JasperPrint print = JasperFillManager.fillReport(
                reporte, parametros, new JRBeanCollectionDataSource(filas));
        return JasperExportManager.exportReportToPdf(print);
    }

    @Override
    public Object datos(ParametrosReporte params) {
        List<GananciaFila> filas = filas(params);
        BigDecimal totalIngresos = suma(filas, GananciaFila::getIngresos);
        BigDecimal totalGastos = suma(filas, GananciaFila::getGastos);
        Map<String, Object> totales = new LinkedHashMap<>();
        totales.put("ingresos", totalIngresos);
        totales.put("gastos", totalGastos);
        totales.put("ganancia", totalIngresos.subtract(totalGastos));
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("desde", params.getDesde());
        resultado.put("hasta", params.getHasta());
        resultado.put("filas", filas);
        resultado.put("totales", totales);
        return resultado;
    }

    List<GananciaFila> filas(ParametrosReporte params) {
        Map<UUID, BigDecimal> porSucursalIngresos = mapa(
                ingresos.sumarPorSucursal(params.getDesde(), params.getHasta()));
        Map<UUID, BigDecimal> porSucursalGastos = mapa(
                gastos.sumarPorSucursal(params.getDesde(), params.getHasta()));
        List<GananciaFila> filas = new ArrayList<>();
        sucursales.findAllByOrderByNombreAsc().forEach(sucursal -> {
            if (params.getSucursalId() != null && !params.getSucursalId().equals(sucursal.getId())) {
                return;
            }
            BigDecimal ingreso = porSucursalIngresos.getOrDefault(sucursal.getId(), BigDecimal.ZERO);
            BigDecimal gasto = porSucursalGastos.getOrDefault(sucursal.getId(), BigDecimal.ZERO);
            filas.add(GananciaFila.builder()
                    .sucursalId(sucursal.getId())
                    .sucursal(sucursal.getNombre())
                    .ingresos(ingreso)
                    .gastos(gasto)
                    .ganancia(ingreso.subtract(gasto))
                    .build());
        });
        return filas;
    }

    private static Map<UUID, BigDecimal> mapa(List<Object[]> grupos) {
        Map<UUID, BigDecimal> mapa = new HashMap<>();
        for (Object[] grupo : grupos) {
            mapa.put((UUID) grupo[0], grupo[1] != null ? (BigDecimal) grupo[1] : BigDecimal.ZERO);
        }
        return mapa;
    }

    private static BigDecimal suma(List<GananciaFila> filas,
                                   java.util.function.Function<GananciaFila, BigDecimal> campo) {
        return filas.stream().map(campo).reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
