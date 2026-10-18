package gt.cunoc.proyecto2.reporting.reportes.generadores;

import gt.cunoc.proyecto2.reporting.repositorios.BusRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.GastoRepositorio;
import gt.cunoc.proyecto2.reporting.reportes.CompiladorJasper;
import gt.cunoc.proyecto2.reporting.reportes.GeneradorReporte;
import gt.cunoc.proyecto2.reporting.reportes.ParametrosReporte;
import gt.cunoc.proyecto2.reporting.reportes.TipoReporte;
import gt.cunoc.proyecto2.reporting.reportes.filas.TallerBusFila;
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
 * RS6 · Gastos de taller por bus.
 * Cada renglón es un gasto del bus (categoría, fecha, descripción, tipo y monto), de modo que las
 * reparaciones repetidas de una misma categoría se ven una a una; se agrupa por bus con su total.
 */
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
        BigDecimal total = filas.stream().map(TallerBusFila::getMonto)
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
        Map<UUID, List<Map<String, Object>>> porBus = new LinkedHashMap<>();
        Map<UUID, String> placas = new HashMap<>();
        for (TallerBusFila f : filas) {
            porBus.computeIfAbsent(f.getBusId(), k -> new ArrayList<>()).add(gastoJson(f));
            placas.putIfAbsent(f.getBusId(), f.getPlaca());
        }
        List<Map<String, Object>> busesJson = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (var grupo : porBus.entrySet()) {
            BigDecimal subtotal = grupo.getValue().stream().map(g -> (BigDecimal) g.get("monto"))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            total = total.add(subtotal);
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("busId", grupo.getKey());
            b.put("placa", placas.get(grupo.getKey()));
            b.put("gastos", grupo.getValue());
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
        for (Object[] gasto : gastos.tallerDetalleBus(
                params.getSucursalId(), params.getBusId(), params.getCategoriaId(),
                params.getDesde(), params.getHasta())) {
            UUID busId = (UUID) gasto[0];
            filas.add(TallerBusFila.builder()
                    .busId(busId)
                    .categoriaId((UUID) gasto[1])
                    .placa(busId != null ? placas.getOrDefault(busId.toString(), "-") : "-")
                    .categoria(gasto[2] != null ? (String) gasto[2] : "-")
                    .fecha(Formatos.fecha((LocalDate) gasto[3]))
                    .descripcion(gasto[4] != null ? (String) gasto[4] : "-")
                    .tipo(tipoTexto((String) gasto[5]))
                    .monto(gasto[6] != null ? (BigDecimal) gasto[6] : BigDecimal.ZERO)
                    .build());
        }
        return filas;
    }

    /** Texto del tipo de gasto para pantalla y PDF. */
    private static String tipoTexto(String tipo) {
        return switch (tipo == null ? "" : tipo) {
            case "MANO_OBRA" -> "Mano de obra";
            case "REPUESTO" -> "Repuestos";
            default -> tipo != null ? tipo : "-";
        };
    }

    private Map<String, Object> gastoJson(TallerBusFila f) {
        Map<String, Object> g = new LinkedHashMap<>();
        g.put("categoriaId", f.getCategoriaId());
        g.put("categoria", f.getCategoria());
        g.put("fecha", f.getFecha());
        g.put("descripcion", f.getDescripcion());
        g.put("tipo", f.getTipo());
        g.put("monto", f.getMonto());
        return g;
    }
}
