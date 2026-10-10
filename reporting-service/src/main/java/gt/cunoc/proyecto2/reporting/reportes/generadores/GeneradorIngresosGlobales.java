package gt.cunoc.proyecto2.reporting.reportes.generadores;

import gt.cunoc.proyecto2.reporting.modelos.IngresoEntidad;
import gt.cunoc.proyecto2.reporting.repositorios.IngresoRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.SucursalRepositorio;
import gt.cunoc.proyecto2.reporting.reportes.CompiladorJasper;
import gt.cunoc.proyecto2.reporting.reportes.GeneradorReporte;
import gt.cunoc.proyecto2.reporting.reportes.ParametrosReporte;
import gt.cunoc.proyecto2.reporting.reportes.TipoReporte;
import gt.cunoc.proyecto2.reporting.reportes.filas.IngresoGlobalFila;
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

/** RG1 · Ingresos por sucursal: listado, subtotal por sucursal y total global. */
@Component
public class GeneradorIngresosGlobales implements GeneradorReporte {

    private final IngresoRepositorio ingresos;
    private final SucursalRepositorio sucursales;
    private final CompiladorJasper compilador;

    public GeneradorIngresosGlobales(IngresoRepositorio ingresos, SucursalRepositorio sucursales,
                                     CompiladorJasper compilador) {
        this.ingresos = ingresos;
        this.sucursales = sucursales;
        this.compilador = compilador;
    }

    @Override
    public TipoReporte tipo() {
        return TipoReporte.INGRESOS;
    }

    @Override
    public byte[] generar(ParametrosReporte params) throws Exception {
        List<IngresoGlobalFila> filas = filas(params);
        BigDecimal total = filas.stream().map(IngresoGlobalFila::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        JasperReport reporte = compilador.obtener("ingresos");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("REPORT_TITLE", "Reporte de Ingresos por Sucursal");
        parametros.put("FROM_DATE", Formatos.texto(params.getDesde()));
        parametros.put("TO_DATE", Formatos.texto(params.getHasta()));
        parametros.put("TOTAL_GLOBAL", Formatos.quetzales(total));

        JasperPrint print = JasperFillManager.fillReport(
                reporte, parametros, new JRBeanCollectionDataSource(filas));
        return JasperExportManager.exportReportToPdf(print);
    }

    @Override
    public Object datos(ParametrosReporte params) {
        Map<String, String> nombres = new HashMap<>();
        sucursales.findAll().forEach(s -> nombres.put(s.getId().toString(), s.getNombre()));
        List<IngresoEntidad> entidades = ingresos.buscar(
                params.getSucursalId(), null, params.getDesde(), params.getHasta()).stream()
                .sorted(Comparator.comparing(IngresoEntidad::getFechaNegocio))
                .toList();

        Map<String, List<Map<String, Object>>> porSucursal = new LinkedHashMap<>();
        Map<String, UUID> ids = new HashMap<>();
        for (IngresoEntidad i : entidades) {
            String clave = String.valueOf(i.getSucursalId());
            ids.putIfAbsent(clave, i.getSucursalId());
            porSucursal.computeIfAbsent(clave, k -> new ArrayList<>()).add(fila(i));
        }

        List<Map<String, Object>> grupos = new ArrayList<>();
        BigDecimal totalGlobal = BigDecimal.ZERO;
        for (var grupo : sucursales.findAllByOrderByNombreAsc()) {
            String clave = grupo.getId().toString();
            if (params.getSucursalId() != null && !params.getSucursalId().equals(grupo.getId())) {
                continue;
            }
            List<Map<String, Object>> filasGrupo = porSucursal.getOrDefault(clave, List.of());
            BigDecimal subtotal = filasGrupo.stream()
                    .map(f -> (BigDecimal) f.get("monto")).reduce(BigDecimal.ZERO, BigDecimal::add);
            totalGlobal = totalGlobal.add(subtotal);
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("sucursalId", grupo.getId());
            g.put("sucursal", grupo.getNombre());
            g.put("filas", filasGrupo);
            g.put("subtotal", subtotal);
            grupos.add(g);
        }
        ids.keySet().removeAll(grupos.stream().map(g -> g.get("sucursalId").toString()).toList());

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("desde", params.getDesde());
        resultado.put("hasta", params.getHasta());
        resultado.put("sucursales", grupos);
        resultado.put("totalGlobal", totalGlobal);
        return resultado;
    }

    List<IngresoGlobalFila> filas(ParametrosReporte params) {
        Map<String, String> nombres = new HashMap<>();
        sucursales.findAll().forEach(s -> nombres.put(s.getId().toString(), s.getNombre()));
        return ingresos.buscar(params.getSucursalId(), null, params.getDesde(), params.getHasta()).stream()
                .sorted(Comparator.comparing(IngresoEntidad::getFechaNegocio))
                .map(i -> IngresoGlobalFila.builder()
                        .fecha(Formatos.fecha(i.getFechaNegocio()))
                        .sucursal(nombres.getOrDefault(String.valueOf(i.getSucursalId()), "-"))
                        .tipo(i.getTipo())
                        .referenciaId(i.getReferenciaId())
                        .detalle(detalle(i))
                        .clienteNit(i.getClienteNit())
                        .monto(i.getMonto())
                        .build())
                .toList();
    }

    private Map<String, Object> fila(IngresoEntidad i) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("fecha", i.getFechaNegocio() != null ? i.getFechaNegocio().toString() : null);
        f.put("tipo", i.getTipo());
        f.put("referenciaId", i.getReferenciaId());
        f.put("detalle", detalle(i));
        f.put("clienteNit", i.getClienteNit());
        f.put("monto", i.getMonto());
        return f;
    }

    private String detalle(IngresoEntidad i) {
        if ("ALQUILER".equals(i.getTipo())) {
            return Formatos.ruta(i.getOrigen(), i.getDestino());
        }
        if (i.getMonto() != null && i.getMonto().signum() < 0) {
            return "Cancelación";
        }
        return Formatos.ruta(i.getOrigen(), i.getDestino()) + " · " + i.getCantidadBoletos() + " boletos";
    }
}
