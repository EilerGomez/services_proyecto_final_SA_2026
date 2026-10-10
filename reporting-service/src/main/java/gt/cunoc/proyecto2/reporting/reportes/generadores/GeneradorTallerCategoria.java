package gt.cunoc.proyecto2.reporting.reportes.generadores;

import gt.cunoc.proyecto2.reporting.repositorios.GastoRepositorio;
import gt.cunoc.proyecto2.reporting.reportes.CompiladorJasper;
import gt.cunoc.proyecto2.reporting.reportes.GeneradorReporte;
import gt.cunoc.proyecto2.reporting.reportes.ParametrosReporte;
import gt.cunoc.proyecto2.reporting.reportes.TipoReporte;
import gt.cunoc.proyecto2.reporting.reportes.filas.TallerCategoriaFila;
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

/** RG5 · Gastos de taller por categoría (filtro por sucursal o bus). */
@Component
public class GeneradorTallerCategoria implements GeneradorReporte {

    private final GastoRepositorio gastos;
    private final CompiladorJasper compilador;

    public GeneradorTallerCategoria(GastoRepositorio gastos, CompiladorJasper compilador) {
        this.gastos = gastos;
        this.compilador = compilador;
    }

    @Override
    public TipoReporte tipo() {
        return TipoReporte.TALLER_POR_CATEGORIA;
    }

    @Override
    public byte[] generar(ParametrosReporte params) throws Exception {
        List<TallerCategoriaFila> filas = filas(params);
        BigDecimal total = filas.stream().map(TallerCategoriaFila::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        JasperReport reporte = compilador.obtener("taller_categoria");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("REPORT_TITLE", "Gastos de Taller por Categoría");
        parametros.put("FROM_DATE", Formatos.texto(params.getDesde()));
        parametros.put("TO_DATE", Formatos.texto(params.getHasta()));
        parametros.put("TOTAL", Formatos.quetzales(total));

        JasperPrint print = JasperFillManager.fillReport(
                reporte, parametros, new JRBeanCollectionDataSource(filas));
        return JasperExportManager.exportReportToPdf(print);
    }

    @Override
    public Object datos(ParametrosReporte params) {
        List<TallerCategoriaFila> filas = filas(params);
        List<Map<String, Object>> json = new ArrayList<>();
        filas.forEach(f -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("categoriaId", f.getCategoriaId());
            fila.put("categoria", f.getCategoria());
            fila.put("manoObra", f.getManoObra());
            fila.put("repuestos", f.getRepuestos());
            fila.put("total", f.getTotal());
            json.add(fila);
        });
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("filas", json);
        resultado.put("total", filas.stream().map(TallerCategoriaFila::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return resultado;
    }

    List<TallerCategoriaFila> filas(ParametrosReporte params) {
        List<TallerCategoriaFila> filas = new ArrayList<>();
        for (Object[] grupo : gastos.tallerPorCategoria(
                params.getSucursalId(), params.getBusId(), params.getDesde(), params.getHasta())) {
            BigDecimal manoObra = grupo[2] != null ? (BigDecimal) grupo[2] : BigDecimal.ZERO;
            BigDecimal repuestos = grupo[3] != null ? (BigDecimal) grupo[3] : BigDecimal.ZERO;
            filas.add(TallerCategoriaFila.builder()
                    .categoriaId((UUID) grupo[0])
                    .categoria(grupo[1] != null ? (String) grupo[1] : "-")
                    .manoObra(manoObra)
                    .repuestos(repuestos)
                    .total(grupo[4] != null ? (BigDecimal) grupo[4] : manoObra.add(repuestos))
                    .build());
        }
        return filas;
    }
}
