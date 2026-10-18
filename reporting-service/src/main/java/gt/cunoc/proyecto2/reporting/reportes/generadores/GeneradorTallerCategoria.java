package gt.cunoc.proyecto2.reporting.reportes.generadores;

import gt.cunoc.proyecto2.reporting.repositorios.BusRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.GastoRepositorio;
import gt.cunoc.proyecto2.reporting.reportes.CompiladorJasper;
import gt.cunoc.proyecto2.reporting.reportes.GeneradorReporte;
import gt.cunoc.proyecto2.reporting.reportes.ParametrosReporte;
import gt.cunoc.proyecto2.reporting.reportes.TipoReporte;
import gt.cunoc.proyecto2.reporting.reportes.filas.TallerCategoriaFila;
import java.math.BigDecimal;
import java.time.LocalDate;
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

/**
 * RG5 · Gastos de taller por categoría (filtro por sucursal o bus).
 * Muestra el detalle de cada gasto (bus, fecha, descripción y monto), con un
 * subtotal por categoría y un gran total.
 */
@Component
public class GeneradorTallerCategoria implements GeneradorReporte {

    private final GastoRepositorio gastos;
    private final BusRepositorio buses;
    private final CompiladorJasper compilador;

    public GeneradorTallerCategoria(GastoRepositorio gastos, BusRepositorio buses,
                                    CompiladorJasper compilador) {
        this.gastos = gastos;
        this.buses = buses;
        this.compilador = compilador;
    }

    @Override
    public TipoReporte tipo() {
        return TipoReporte.TALLER_POR_CATEGORIA;
    }

    @Override
    public byte[] generar(ParametrosReporte params) throws Exception {
        List<TallerCategoriaFila> filas = filas(params);

        JasperReport reporte = compilador.obtener("taller_categoria");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("REPORT_TITLE", "Gastos de Taller por Categoría");
        parametros.put("FROM_DATE", Formatos.texto(params.getDesde()));
        parametros.put("TO_DATE", Formatos.texto(params.getHasta()));
        parametros.put("TOTAL", Formatos.quetzales(total(filas)));

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
            fila.put("categoria", f.getCategoria());
            fila.put("placa", f.getPlaca());
            fila.put("fecha", f.getFecha());
            fila.put("descripcion", f.getDescripcion());
            fila.put("monto", f.getMonto());
            json.add(fila);
        });
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("filas", json);
        resultado.put("total", total(filas));
        return resultado;
    }

    List<TallerCategoriaFila> filas(ParametrosReporte params) {
        Map<String, String> placas = new HashMap<>();
        buses.findAll().forEach(b -> placas.put(b.getId().toString(), b.getPlaca()));
        List<TallerCategoriaFila> filas = new ArrayList<>();
        for (Object[] gasto : gastos.tallerDetalleCategoria(
                params.getSucursalId(), params.getBusId(), params.getDesde(), params.getHasta())) {
            UUID busId = (UUID) gasto[2];
            filas.add(TallerCategoriaFila.builder()
                    .categoria(gasto[1] != null ? (String) gasto[1] : "-")
                    .placa(busId != null ? placas.getOrDefault(busId.toString(), "-") : "-")
                    .fecha(Formatos.fecha((LocalDate) gasto[3]))
                    .descripcion(gasto[4] != null ? (String) gasto[4] : "-")
                    .monto(gasto[5] != null ? (BigDecimal) gasto[5] : BigDecimal.ZERO)
                    .build());
        }
        return filas;
    }

    private BigDecimal total(List<TallerCategoriaFila> filas) {
        return filas.stream().map(TallerCategoriaFila::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
