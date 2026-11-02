package gt.cunoc.proyecto2.reporting.reportes.generadores;

import gt.cunoc.proyecto2.reporting.repositorios.ChoferRepositorio;
import gt.cunoc.proyecto2.reporting.reportes.CompiladorJasper;
import gt.cunoc.proyecto2.reporting.reportes.GeneradorReporte;
import gt.cunoc.proyecto2.reporting.reportes.ParametrosReporte;
import gt.cunoc.proyecto2.reporting.reportes.TipoReporte;
import gt.cunoc.proyecto2.reporting.reportes.filas.ChoferFila;
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

/** Listado de choferes de la sucursal. */
@Component
public class GeneradorChoferes implements GeneradorReporte {

    private final ChoferRepositorio choferes;
    private final CompiladorJasper compilador;

    // Inyecta los repositorios y el compilador de plantillas Jasper.
    public GeneradorChoferes(ChoferRepositorio choferes, CompiladorJasper compilador) {
        this.choferes = choferes;
        this.compilador = compilador;
    }

    @Override
    public TipoReporte tipo() {
        return TipoReporte.CHOFERES;
    }

    // Genera el PDF del reporte con los datos y los filtros recibidos.
    @Override
    public byte[] generar(ParametrosReporte params) throws Exception {
        List<ChoferFila> filas = filas(params);

        JasperReport reporte = compilador.obtener("choferes");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("REPORT_TITLE", "Choferes de la Sucursal");
        parametros.put("TOTAL_CHOFERES", String.valueOf(filas.size()));

        JasperPrint print = JasperFillManager.fillReport(
                reporte, parametros, new JRBeanCollectionDataSource(filas));
        return JasperExportManager.exportReportToPdf(print);
    }

    // Devuelve los mismos datos en JSON para la API.
    @Override
    public Object datos(ParametrosReporte params) {
        return filas(params).stream().map(f -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("choferId", f.getChoferId());
            fila.put("nombreCompleto", f.getNombreCompleto());
            fila.put("numeroLicencia", f.getNumeroLicencia());
            fila.put("tipoLicencia", f.getTipoLicencia());
            fila.put("fechaVencimientoLicencia", f.getFechaVencimientoLicencia());
            fila.put("estado", f.getEstado());
            fila.put("totalViajes", f.getTotalViajes());
            return fila;
        }).toList();
    }

    // Arma las filas del reporte a partir de las consultas con los filtros.
    List<ChoferFila> filas(ParametrosReporte params) {
        return choferes.findBySucursalIdOrderByNombreCompletoAsc(params.getSucursalId()).stream()
                .filter(c -> params.getEstado() == null || params.getEstado().equals(c.getEstado()))
                .map(c -> ChoferFila.builder()
                        .choferId(c.getId())
                        .nombreCompleto(c.getNombreCompleto())
                        .numeroLicencia(c.getNumeroLicencia())
                        .tipoLicencia(c.getTipoLicencia())
                        .fechaVencimientoLicencia(c.getFechaVencimientoLicencia() != null
                                ? c.getFechaVencimientoLicencia().toString() : "-")
                        .estado(c.getEstado())
                        .totalViajes(c.getTotalViajes())
                        .build())
                .toList();
    }
}
