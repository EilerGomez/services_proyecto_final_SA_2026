package gt.cunoc.proyecto2.reporting.reportes.generadores;

import gt.cunoc.proyecto2.reporting.modelos.SucursalEntidad;
import gt.cunoc.proyecto2.reporting.repositorios.GastoRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.SucursalRepositorio;
import gt.cunoc.proyecto2.reporting.reportes.CompiladorJasper;
import gt.cunoc.proyecto2.reporting.reportes.GeneradorReporte;
import gt.cunoc.proyecto2.reporting.reportes.ParametrosReporte;
import gt.cunoc.proyecto2.reporting.reportes.TipoReporte;
import gt.cunoc.proyecto2.reporting.reportes.filas.GastoGlobalFila;
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

/** RG2 · Gastos: combustible, taller (mano de obra), repuestos, depreciación y salarios. */
@Component
public class GeneradorGastosGlobales implements GeneradorReporte {

    /** gastos.tipo → campo del JSON. */
    static final Map<String, String> CAMPOS = Map.of(
            "COMBUSTIBLE", "combustible",
            "MANO_OBRA", "taller",
            "REPUESTO", "repuestos",
            "DEPRECIACION", "depreciacion",
            "SALARIO_CHOFER", "salarioChoferes");

    private final GastoRepositorio gastos;
    private final SucursalRepositorio sucursales;
    private final CompiladorJasper compilador;

    public GeneradorGastosGlobales(GastoRepositorio gastos, SucursalRepositorio sucursales,
                                   CompiladorJasper compilador) {
        this.gastos = gastos;
        this.sucursales = sucursales;
        this.compilador = compilador;
    }

    @Override
    public TipoReporte tipo() {
        return TipoReporte.GASTOS;
    }

    @Override
    public byte[] generar(ParametrosReporte params) throws Exception {
        List<GastoGlobalFila> filas = filas(params);
        BigDecimal total = filas.stream().map(GastoGlobalFila::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        JasperReport reporte = compilador.obtener("gastos");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("REPORT_TITLE", "Reporte de Gastos");
        parametros.put("FROM_DATE", Formatos.texto(params.getDesde()));
        parametros.put("TO_DATE", Formatos.texto(params.getHasta()));
        parametros.put("TOTAL", Formatos.quetzales(total));

        JasperPrint print = JasperFillManager.fillReport(
                reporte, parametros, new JRBeanCollectionDataSource(filas));
        return JasperExportManager.exportReportToPdf(print);
    }

    @Override
    public Object datos(ParametrosReporte params) {
        Map<UUID, Map<String, BigDecimal>> porSucursal = new HashMap<>();
        for (Object[] grupo : gastos.sumarPorSucursalYTipo(
                params.getSucursalId(), params.getDesde(), params.getHasta())) {
            porSucursal.computeIfAbsent((UUID) grupo[0], k -> new HashMap<>())
                    .put((String) grupo[1], grupo[2] != null ? (BigDecimal) grupo[2] : BigDecimal.ZERO);
        }
        List<Map<String, Object>> filas = new ArrayList<>();
        Map<String, BigDecimal> totales = base();
        for (SucursalEntidad s : sucursales.findAllByOrderByNombreAsc()) {
            if (params.getSucursalId() != null && !params.getSucursalId().equals(s.getId())) {
                continue;
            }
            Map<String, BigDecimal> montos = porSucursal.getOrDefault(s.getId(), Map.of());
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("sucursalId", s.getId());
            f.put("sucursal", s.getNombre());
            BigDecimal total = BigDecimal.ZERO;
            for (var campo : CAMPOS.entrySet()) {
                BigDecimal monto = cero(montos.get(campo.getKey()));
                f.put(campo.getValue(), monto);
                totales.merge(campo.getValue(), monto, BigDecimal::add);
                total = total.add(monto);
            }
            f.put("total", total);
            totales.merge("total", total, BigDecimal::add);
            filas.add(f);
        }
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("desde", params.getDesde());
        resultado.put("hasta", params.getHasta());
        resultado.put("porSucursal", filas);
        resultado.put("totales", totales);
        return resultado;
    }

    List<GastoGlobalFila> filas(ParametrosReporte params) {
        Map<String, String> nombres = new HashMap<>();
        sucursales.findAll().forEach(s -> nombres.put(s.getId().toString(), s.getNombre()));
        List<GastoGlobalFila> filas = new ArrayList<>();
        for (Object[] grupo : gastos.sumarPorSucursalYTipo(
                params.getSucursalId(), params.getDesde(), params.getHasta())) {
            filas.add(GastoGlobalFila.builder()
                    .sucursal(nombres.getOrDefault(String.valueOf(grupo[0]), "-"))
                    .tipo((String) grupo[1])
                    .monto(grupo[2] != null ? (BigDecimal) grupo[2] : BigDecimal.ZERO)
                    .build());
        }
        return filas;
    }

    private static Map<String, BigDecimal> base() {
        Map<String, BigDecimal> totales = new LinkedHashMap<>();
        CAMPOS.values().forEach(c -> totales.put(c, BigDecimal.ZERO));
        totales.put("total", BigDecimal.ZERO);
        return totales;
    }

    private static BigDecimal cero(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }
}
