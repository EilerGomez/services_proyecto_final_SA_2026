package gt.cunoc.proyecto2.reporting.reportes.generadores;

import gt.cunoc.proyecto2.reporting.repositorios.BusRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.GastoRepositorio;
import gt.cunoc.proyecto2.reporting.reportes.CompiladorJasper;
import gt.cunoc.proyecto2.reporting.reportes.GeneradorReporte;
import gt.cunoc.proyecto2.reporting.reportes.ParametrosReporte;
import gt.cunoc.proyecto2.reporting.reportes.TipoReporte;
import gt.cunoc.proyecto2.reporting.reportes.filas.TallerBusFila;
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

/** RS6 · Gastos de taller por bus, agrupados por categoría. */
@Component
public class GeneradorTallerBus implements GeneradorReporte {

    private final GastoRepositorio gastos;
    private final BusRepositorio buses;
    private final CompiladorJasper compilador;

    public GeneradorTallerBus(GastoRepositorio gastos, BusRepositorio buses,
                              CompiladorJasper compilador) {
        this.gastos = gastos;
        this.buses = buses;
        this.compilador = compilador;
    }

    @Override
    public TipoReporte tipo() {
        return TipoReporte.TALLER_POR_BUS;
    }

    @Override
    public byte[] generar(ParametrosReporte params) throws Exception {
        List<TallerBusFila> filas = filas(params);
        BigDecimal total = filas.stream().map(TallerBusFila::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        JasperReport reporte = compilador.obtener("taller_bus");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("REPORT_TITLE", "Gastos de Taller por Bus");
        parametros.put("FROM_DATE", Formatos.texto(params.getDesde()));
        parametros.put("TO_DATE", Formatos.texto(params.getHasta()));
        parametros.put("TOTAL", Formatos.quetzales(total));

        JasperPrint print = JasperFillManager.fillReport(
                reporte, parametros, new JRBeanCollectionDataSource(filas));
        return JasperExportManager.exportReportToPdf(print);
    }

    @Override
    public Object datos(ParametrosReporte params) {
        List<TallerBusFila> filas = filas(params);
        Map<String, List<Map<String, Object>>> porBus = new LinkedHashMap<>();
        Map<String, String> placas = new HashMap<>();
        for (TallerBusFila f : filas) {
            String clave = String.valueOf(f.getBusId());
            placas.putIfAbsent(clave, f.getPlaca());
            porBus.computeIfAbsent(clave, k -> new ArrayList<>()).add(categoriaJson(f));
        }
        List<Map<String, Object>> busesJson = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (var grupo : porBus.entrySet()) {
            BigDecimal subtotal = grupo.getValue().stream().map(c -> (BigDecimal) c.get("total"))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            total = total.add(subtotal);
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("busId", UUID.fromString(grupo.getKey()));
            b.put("placa", placas.get(grupo.getKey()));
            b.put("categorias", grupo.getValue());
            b.put("total", subtotal);
            busesJson.add(b);
        }
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("buses", busesJson);
        resultado.put("total", total);
        return resultado;
    }

    List<TallerBusFila> filas(ParametrosReporte params) {
        Map<String, String> placas = new HashMap<>();
        buses.findAll().forEach(b -> placas.put(b.getId().toString(), b.getPlaca()));
        List<TallerBusFila> filas = new ArrayList<>();
        for (Object[] grupo : gastos.tallerPorBusYCategoria(
                params.getSucursalId(), params.getBusId(), params.getDesde(), params.getHasta())) {
            UUID busId = (UUID) grupo[0];
            BigDecimal manoObra = grupo[3] != null ? (BigDecimal) grupo[3] : BigDecimal.ZERO;
            BigDecimal repuestos = grupo[4] != null ? (BigDecimal) grupo[4] : BigDecimal.ZERO;
            filas.add(TallerBusFila.builder()
                    .busId(busId)
                    .placa(placas.getOrDefault(String.valueOf(busId), "-"))
                    .categoria(grupo[2] != null ? (String) grupo[2] : "-")
                    .manoObra(manoObra)
                    .repuestos(repuestos)
                    .total(grupo[5] != null ? (BigDecimal) grupo[5] : manoObra.add(repuestos))
                    .build());
        }
        return filas;
    }

    private Map<String, Object> categoriaJson(TallerBusFila f) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("categoria", f.getCategoria());
        c.put("manoObra", f.getManoObra());
        c.put("repuestos", f.getRepuestos());
        c.put("total", f.getTotal());
        return c;
    }
}
