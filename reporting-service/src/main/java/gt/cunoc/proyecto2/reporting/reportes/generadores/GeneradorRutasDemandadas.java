package gt.cunoc.proyecto2.reporting.reportes.generadores;

import gt.cunoc.proyecto2.reporting.repositorios.IngresoRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.RutaRepositorio;
import gt.cunoc.proyecto2.reporting.reportes.CompiladorJasper;
import gt.cunoc.proyecto2.reporting.reportes.GeneradorReporte;
import gt.cunoc.proyecto2.reporting.reportes.ParametrosReporte;
import gt.cunoc.proyecto2.reporting.reportes.TipoReporte;
import gt.cunoc.proyecto2.reporting.reportes.filas.RutaDemandaFila;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/** Rutas más demandadas por boletos (las cancelaciones restan). */
@Component
public class GeneradorRutasDemandadas implements GeneradorReporte {

    private final IngresoRepositorio ingresos;
    private final RutaRepositorio rutas;
    private final CompiladorJasper compilador;

    // Inyecta los repositorios y el compilador de plantillas Jasper.
    public GeneradorRutasDemandadas(IngresoRepositorio ingresos, RutaRepositorio rutas,
                                    CompiladorJasper compilador) {
        this.ingresos = ingresos;
        this.rutas = rutas;
        this.compilador = compilador;
    }

    @Override
    public TipoReporte tipo() {
        return TipoReporte.RUTAS_DEMANDADAS;
    }

    // Genera el PDF del reporte con los datos y los filtros recibidos.
    @Override
    public byte[] generar(ParametrosReporte params) throws Exception {
        List<RutaDemandaFila> filas = filas(params);

        JasperReport reporte = compilador.obtener("rutas_demandadas");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("REPORT_TITLE", "Rutas Más Demandadas");
        parametros.put("FROM_DATE", Formatos.texto(params.getDesde()));
        parametros.put("TO_DATE", Formatos.texto(params.getHasta()));
        parametros.put("SUCURSAL", params.getSucursalId() == null
                ? "Todas las sucursales" : "Sucursal " + params.getSucursalId());

        JasperPrint print = JasperFillManager.fillReport(
                reporte, parametros, new JRBeanCollectionDataSource(filas));
        return JasperExportManager.exportReportToPdf(print);
    }

    // Devuelve los mismos datos en JSON para la API.
    @Override
    public Object datos(ParametrosReporte params) {
        return filas(params).stream().map(f -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("rutaId", f.getRutaId());
            fila.put("origen", f.getOrigen());
            fila.put("destino", f.getDestino());
            fila.put("boletosVendidos", f.getBoletosVendidos());
            fila.put("boletosCancelados", f.getBoletosCancelados());
            fila.put("boletos", f.getBoletos());
            fila.put("ingresos", f.getIngresos());
            return fila;
        }).toList();
    }

    // Arma las filas del reporte a partir de las consultas con los filtros.
    List<RutaDemandaFila> filas(ParametrosReporte params) {
        Map<String, String[]> nombres = new HashMap<>();
        rutas.findAll().forEach(r -> nombres.put(r.getId().toString(),
                new String[]{r.getOrigenNombre(), r.getDestinoNombre()}));
        int limite = params.getLimite() != null && params.getLimite() > 0 ? params.getLimite() : 10;
        List<RutaDemandaFila> filas = new ArrayList<>();
        for (Object[] grupo : ingresos.demandaPorRuta(
                params.getSucursalId(), params.getDesde(), params.getHasta(), PageRequest.of(0, limite))) {
            UUID rutaId = (UUID) grupo[0];
            String[] nombre = nombres.getOrDefault(rutaId.toString(), new String[]{"-", "-"});
            filas.add(RutaDemandaFila.builder()
                    .rutaId(rutaId)
                    .origen(nombre[0])
                    .destino(nombre[1])
                    .boletosVendidos(grupo[1] != null ? ((Number) grupo[1]).longValue() : 0L)
                    .boletosCancelados(grupo[2] != null ? ((Number) grupo[2]).longValue() : 0L)
                    .boletos(grupo[3] != null ? ((Number) grupo[3]).longValue() : 0L)
                    .ingresos(grupo[4] != null ? (BigDecimal) grupo[4] : BigDecimal.ZERO)
                    .build());
        }
        return filas;
    }
}
