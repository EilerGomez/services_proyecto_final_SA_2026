package gt.cunoc.proyecto2.reporting.reportes.generadores;

import gt.cunoc.proyecto2.reporting.reportes.CompiladorJasper;
import gt.cunoc.proyecto2.reporting.reportes.GeneradorReporte;
import gt.cunoc.proyecto2.reporting.reportes.ParametrosReporte;
import gt.cunoc.proyecto2.reporting.reportes.TipoReporte;
import gt.cunoc.proyecto2.reporting.reportes.filas.TallerBusCategoriaFila;
import gt.cunoc.proyecto2.reporting.reportes.filas.TallerBusFila;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
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
 * RS6 agrupado · Gastos de taller por bus, sumados por categoría.
 *
 * Los mismos gastos que lista RS6, pero sin el detalle: por cada bus va una línea por categoría con
 * cuántos gastos hubo y cuánto suman, más el total del bus. Así se ve de un vistazo en qué se le va
 * el dinero a cada unidad, que es para lo que sirve el reporte: un bus que acumula en Motor es
 * candidato a desactivarse.
 *
 * Reutiliza las filas de {@link GeneradorTallerBus} para no repetir la consulta ni las reglas de
 * placa y tipo de gasto: lo único propio de aquí es la agrupación.
 */
@Component
public class GeneradorTallerBusCategoria implements GeneradorReporte {

    private final GeneradorTallerBus detalle;
    private final CompiladorJasper compilador;

    public GeneradorTallerBusCategoria(GeneradorTallerBus detalle, CompiladorJasper compilador) {
        this.detalle = detalle;
        this.compilador = compilador;
    }

    @Override
    public TipoReporte tipo() {
        return TipoReporte.TALLER_BUS_CATEGORIA;
    }

    @Override
    public byte[] generar(ParametrosReporte params) throws Exception {
        List<TallerBusCategoriaFila> filas = filas(params);
        BigDecimal total = filas.stream().map(TallerBusCategoriaFila::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        JasperReport reporte = compilador.obtener("taller_bus_categoria");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("REPORT_TITLE", "Gastos de Taller por Bus y Categoría");
        parametros.put("FROM_DATE", Formatos.texto(params.getDesde()));
        parametros.put("TO_DATE", Formatos.texto(params.getHasta()));
        parametros.put("TOTAL", Formatos.quetzales(total));

        JasperPrint print = JasperFillManager.fillReport(
                reporte, parametros, new JRBeanCollectionDataSource(filas));
        return JasperExportManager.exportReportToPdf(print);
    }

    @Override
    public Object datos(ParametrosReporte params) {
        Map<UUID, List<Map<String, Object>>> porBus = new LinkedHashMap<>();
        Map<UUID, String> placas = new LinkedHashMap<>();
        for (TallerBusCategoriaFila f : filas(params)) {
            porBus.computeIfAbsent(f.getBusId(), k -> new ArrayList<>()).add(categoriaJson(f));
            placas.putIfAbsent(f.getBusId(), f.getPlaca());
        }
        List<Map<String, Object>> busesJson = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (var grupo : porBus.entrySet()) {
            BigDecimal subtotal = grupo.getValue().stream().map(c -> (BigDecimal) c.get("monto"))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            total = total.add(subtotal);
            Map<String, Object> bus = new LinkedHashMap<>();
            bus.put("busId", grupo.getKey());
            bus.put("placa", placas.get(grupo.getKey()));
            bus.put("categorias", grupo.getValue());
            bus.put("total", subtotal);
            busesJson.add(bus);
        }
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("buses", busesJson);
        resultado.put("total", total);
        return resultado;
    }

    /**
     * Une los gastos de un mismo bus y categoría. Dentro de cada bus las categorías salen de mayor
     * a menor gasto: arriba queda en lo que más se le ha invertido, que es lo que se anda buscando.
     */
    List<TallerBusCategoriaFila> filas(ParametrosReporte params) {
        Map<UUID, Map<String, TallerBusCategoriaFila>> porBus = new LinkedHashMap<>();
        for (TallerBusFila gasto : detalle.filas(params)) {
            Map<String, TallerBusCategoriaFila> categorias =
                    porBus.computeIfAbsent(gasto.getBusId(), k -> new LinkedHashMap<>());
            categorias.merge(claveCategoria(gasto), desde(gasto), GeneradorTallerBusCategoria::sumar);
        }
        List<TallerBusCategoriaFila> filas = new ArrayList<>();
        for (Map<String, TallerBusCategoriaFila> categorias : porBus.values()) {
            categorias.values().stream()
                    .sorted(Comparator.comparing(TallerBusCategoriaFila::getMonto).reversed())
                    .forEach(filas::add);
        }
        return filas;
    }

    /** Las categorías viejas pueden venir sin id: entonces agrupan por su nombre. */
    private static String claveCategoria(TallerBusFila gasto) {
        return gasto.getCategoriaId() != null ? gasto.getCategoriaId().toString() : gasto.getCategoria();
    }

    private static TallerBusCategoriaFila desde(TallerBusFila gasto) {
        return TallerBusCategoriaFila.builder()
                .busId(gasto.getBusId())
                .categoriaId(gasto.getCategoriaId())
                .placa(gasto.getPlaca())
                .categoria(gasto.getCategoria())
                .gastos(1)
                .monto(gasto.getMonto() != null ? gasto.getMonto() : BigDecimal.ZERO)
                .build();
    }

    private static TallerBusCategoriaFila sumar(TallerBusCategoriaFila acumulado,
                                                TallerBusCategoriaFila nuevo) {
        return TallerBusCategoriaFila.builder()
                .busId(acumulado.getBusId())
                .categoriaId(acumulado.getCategoriaId())
                .placa(acumulado.getPlaca())
                .categoria(acumulado.getCategoria())
                .gastos(acumulado.getGastos() + nuevo.getGastos())
                .monto(acumulado.getMonto().add(nuevo.getMonto()))
                .build();
    }

    private static Map<String, Object> categoriaJson(TallerBusCategoriaFila f) {
        Map<String, Object> c = new LinkedHashMap<>();
        c.put("categoriaId", f.getCategoriaId());
        c.put("categoria", f.getCategoria());
        c.put("gastos", f.getGastos());
        c.put("monto", f.getMonto());
        return c;
    }
}
