package gt.cunoc.proyecto2.reporting.reportes.generadores;

import gt.cunoc.proyecto2.reporting.modelos.IngresoEntidad;
import gt.cunoc.proyecto2.reporting.repositorios.IngresoRepositorio;
import gt.cunoc.proyecto2.reporting.reportes.CompiladorJasper;
import gt.cunoc.proyecto2.reporting.reportes.GeneradorReporte;
import gt.cunoc.proyecto2.reporting.reportes.ParametrosReporte;
import gt.cunoc.proyecto2.reporting.reportes.TipoReporte;
import gt.cunoc.proyecto2.reporting.reportes.filas.AlquilerFila;
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

/** RS4 · Ingresos por alquiler de la sucursal. */
@Component
public class GeneradorIngresosAlquiler implements GeneradorReporte {

    private final IngresoRepositorio ingresos;
    private final CompiladorJasper compilador;

    public GeneradorIngresosAlquiler(IngresoRepositorio ingresos, CompiladorJasper compilador) {
        this.ingresos = ingresos;
        this.compilador = compilador;
    }

    @Override
    public TipoReporte tipo() {
        return TipoReporte.INGRESOS_ALQUILER;
    }

    @Override
    public byte[] generar(ParametrosReporte params) throws Exception {
        List<AlquilerFila> filas = filas(params);
        BigDecimal total = filas.stream().map(AlquilerFila::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        JasperReport reporte = compilador.obtener("ingresos_alquiler");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("REPORT_TITLE", "Ingresos por Alquiler");
        parametros.put("FROM_DATE", Formatos.texto(params.getDesde()));
        parametros.put("TO_DATE", Formatos.texto(params.getHasta()));
        parametros.put("TOTAL", Formatos.quetzales(total));

        JasperPrint print = JasperFillManager.fillReport(
                reporte, parametros, new JRBeanCollectionDataSource(filas));
        return JasperExportManager.exportReportToPdf(print);
    }

    @Override
    public Object datos(ParametrosReporte params) {
        List<AlquilerFila> filas = filas(params);
        List<Map<String, Object>> json = filas.stream().map(f -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("fecha", f.getFecha());
            fila.put("ordenId", f.getOrdenId());
            fila.put("clienteNit", f.getClienteNit());
            fila.put("clienteNombre", f.getClienteNombre());
            fila.put("origen", f.getOrigen());
            fila.put("destino", f.getDestino());
            fila.put("fechaSalida", f.getFechaSalida());
            fila.put("fechaRetorno", f.getFechaRetorno());
            fila.put("monto", f.getMonto());
            return fila;
        }).toList();
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("filas", json);
        resultado.put("total", filas.stream().map(AlquilerFila::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        return resultado;
    }

    List<AlquilerFila> filas(ParametrosReporte params) {
        return ingresos.buscar(params.getSucursalId(), "ALQUILER", params.getDesde(), params.getHasta())
                .stream()
                .sorted(Comparator.comparing(IngresoEntidad::getFechaNegocio))
                .map(i -> AlquilerFila.builder()
                        .fecha(Formatos.fecha(i.getFechaNegocio()))
                        .fechaSalida(Formatos.fechaHora(i.getFechaSalida()))
                        .fechaRetorno(Formatos.fechaHora(i.getFechaRetorno()))
                        .ordenId(i.getReferenciaId())
                        .clienteNit(i.getClienteNit())
                        .clienteNombre(i.getClienteNombre())
                        .origen(i.getOrigen())
                        .destino(i.getDestino())
                        .monto(i.getMonto())
                        .build())
                .toList();
    }
}
