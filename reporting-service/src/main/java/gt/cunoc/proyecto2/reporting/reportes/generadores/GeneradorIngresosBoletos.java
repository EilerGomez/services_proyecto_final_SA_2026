package gt.cunoc.proyecto2.reporting.reportes.generadores;

import gt.cunoc.proyecto2.reporting.modelos.BusEntidad;
import gt.cunoc.proyecto2.reporting.modelos.IngresoEntidad;
import gt.cunoc.proyecto2.reporting.repositorios.BusRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.IngresoRepositorio;
import gt.cunoc.proyecto2.reporting.reportes.CompiladorJasper;
import gt.cunoc.proyecto2.reporting.reportes.GeneradorReporte;
import gt.cunoc.proyecto2.reporting.reportes.ParametrosReporte;
import gt.cunoc.proyecto2.reporting.reportes.TipoReporte;
import gt.cunoc.proyecto2.reporting.reportes.filas.BoletoFila;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Component;

/** RS3 · Ingresos por boletos de la sucursal (filtros ruta y bus). */
@Component
public class GeneradorIngresosBoletos implements GeneradorReporte {

    private final IngresoRepositorio ingresos;
    private final BusRepositorio buses;
    private final CompiladorJasper compilador;

    public GeneradorIngresosBoletos(IngresoRepositorio ingresos, BusRepositorio buses,
                                    CompiladorJasper compilador) {
        this.ingresos = ingresos;
        this.buses = buses;
        this.compilador = compilador;
    }

    @Override
    public TipoReporte tipo() {
        return TipoReporte.INGRESOS_BOLETOS;
    }

    @Override
    public byte[] generar(ParametrosReporte params) throws Exception {
        List<BoletoFila> filas = filas(params);
        BigDecimal total = filas.stream().map(BoletoFila::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        JasperReport reporte = compilador.obtener("ingresos_boletos");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("REPORT_TITLE", "Ingresos por Boletos");
        parametros.put("FROM_DATE", Formatos.texto(params.getDesde()));
        parametros.put("TO_DATE", Formatos.texto(params.getHasta()));
        parametros.put("TOTAL", Formatos.quetzales(total));
        int vendidos = filas.stream().mapToInt(f -> Math.max(f.getBoletos(), 0)).sum();
        int cancelados = filas.stream().mapToInt(f -> Math.max(-f.getBoletos(), 0)).sum();
        parametros.put("BOLETOS_SUMMARY", "Vendidos: " + vendidos + " · cancelados: " + cancelados
                + " · netos: " + (vendidos - cancelados));

        JasperPrint print = JasperFillManager.fillReport(
                reporte, parametros, new JRBeanCollectionDataSource(filas));
        return JasperExportManager.exportReportToPdf(print);
    }

    @Override
    public Object datos(ParametrosReporte params) {
        List<BoletoFila> filas = filas(params);
        List<Map<String, Object>> json = filas.stream().map(f -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("fecha", f.getFecha());
            fila.put("ventaId", f.getVentaId());
            fila.put("ruta", f.getRuta());
            fila.put("busPlaca", f.getBusPlaca());
            fila.put("clienteNit", f.getClienteNit());
            fila.put("movimiento", f.getMovimiento());
            fila.put("boletos", f.getBoletos());
            fila.put("monto", f.getMonto());
            return fila;
        }).toList();
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("filas", json);
        resultado.put("totalBoletos", filas.stream().map(BoletoFila::getBoletos)
                .reduce(0, Integer::sum));
        resultado.put("boletosVendidos", filas.stream().mapToInt(f -> Math.max(f.getBoletos(), 0)).sum());
        resultado.put("boletosCancelados", filas.stream().mapToInt(f -> Math.max(-f.getBoletos(), 0)).sum());
        resultado.put("total", filas.stream().map(BoletoFila::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return resultado;
    }

    List<BoletoFila> filas(ParametrosReporte params) {
        Map<String, String> placas = new HashMap<>();
        buses.findAll().forEach(b -> placas.put(b.getId().toString(), b.getPlaca()));
        return ingresos.buscar(params.getSucursalId(), "BOLETO", params.getDesde(), params.getHasta())
                .stream()
                .filter(i -> params.getRutaId() == null || params.getRutaId().equals(i.getRutaId()))
                .filter(i -> params.getBusId() == null || params.getBusId().equals(i.getBusId()))
                .sorted(Comparator.comparing(IngresoEntidad::getFechaNegocio))
                .map(i -> BoletoFila.builder()
                        .fecha(Formatos.fecha(i.getFechaNegocio()))
                        .ventaId(i.getReferenciaId())
                        .ruta(Formatos.ruta(i.getOrigen(), i.getDestino()))
                        .busPlaca(i.getBusId() != null
                                ? placas.getOrDefault(i.getBusId().toString(), "-") : "-")
                        .clienteNit(i.getClienteNit())
                        .movimiento(i.getCantidadBoletos() < 0 ? "CANCELACION" : "VENTA")
                        .boletos(i.getCantidadBoletos())
                        .monto(i.getMonto())
                        .build())
                .toList();
    }
}
