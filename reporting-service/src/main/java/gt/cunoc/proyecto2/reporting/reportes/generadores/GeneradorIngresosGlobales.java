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

/** Ingresos por sucursal: listado, subtotal por sucursal y total global. */
@Component
public class GeneradorIngresosGlobales implements GeneradorReporte {

    private final IngresoRepositorio ingresos;
    private final SucursalRepositorio sucursales;
    private final CompiladorJasper compilador;

    // Inyecta los repositorios y el compilador de plantillas Jasper.
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

    // Genera el PDF del reporte con los datos y los filtros recibidos.
    @Override
    public byte[] generar(ParametrosReporte params) throws Exception {
        List<IngresoGlobalFila> filas = filas(params);
        BigDecimal total = filas.stream().map(IngresoGlobalFila::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int vendidos = filas.stream().filter(f -> "BOLETO".equals(f.getTipo()))
                .mapToInt(f -> Math.max(f.getBoletos(), 0)).sum();
        int cancelados = filas.stream().filter(f -> "BOLETO".equals(f.getTipo()))
                .mapToInt(f -> Math.max(-f.getBoletos(), 0)).sum();

        JasperReport reporte = compilador.obtener("ingresos");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("REPORT_TITLE", "Reporte de Ingresos por Sucursal");
        parametros.put("FROM_DATE", Formatos.texto(params.getDesde()));
        parametros.put("TO_DATE", Formatos.texto(params.getHasta()));
        parametros.put("TOTAL_GLOBAL", Formatos.quetzales(total));
        parametros.put("BOLETOS_SUMMARY", "Boletos vendidos: " + vendidos
                + " · cancelados: " + cancelados + " · netos: " + (vendidos - cancelados));

        JasperPrint print = JasperFillManager.fillReport(
                reporte, parametros, new JRBeanCollectionDataSource(filas));
        return JasperExportManager.exportReportToPdf(print);
    }

    // Devuelve los mismos datos en JSON para la API.
    @Override
    public Object datos(ParametrosReporte params) {
        Map<String, String> nombres = new HashMap<>();
        sucursales.findAll().forEach(s -> nombres.put(s.getId().toString(), s.getNombre()));
        List<IngresoEntidad> entidades = ingresos.buscar(
                params.getSucursalId(), null, params.getDesde(), params.getHasta()).stream()
                .sorted(Comparator.comparing(IngresoEntidad::getFechaNegocio))
                .toList();

        Map<String, List<Map<String, Object>>> porSucursal = new LinkedHashMap<>();
        for (IngresoEntidad i : entidades) {
            String clave = String.valueOf(i.getSucursalId());
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
            List<IngresoEntidad> ingresosGrupo = entidades.stream()
                    .filter(i -> grupo.getId().equals(i.getSucursalId()))
                    .toList();
            Map<String, Object> g = new LinkedHashMap<>();
            g.put("sucursalId", grupo.getId());
            g.put("sucursal", grupo.getNombre());
            g.put("filas", filasGrupo);
            g.put("subtotal", subtotal);
            g.putAll(resumenBoletos(ingresosGrupo));
            grupos.add(g);
        }

        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("desde", params.getDesde());
        resultado.put("hasta", params.getHasta());
        resultado.put("sucursales", grupos);
        resultado.put("totalGlobal", totalGlobal);
        resultado.putAll(resumenBoletos(entidades));
        return resultado;
    }

    // Arma las filas del reporte a partir de las consultas con los filtros.
    List<IngresoGlobalFila> filas(ParametrosReporte params) {
        Map<String, String> nombres = new HashMap<>();
        sucursales.findAll().forEach(s -> nombres.put(s.getId().toString(), s.getNombre()));
        return ingresos.buscar(params.getSucursalId(), null, params.getDesde(), params.getHasta()).stream()
                .sorted(Comparator.comparing(IngresoEntidad::getFechaNegocio))
                .map(i -> IngresoGlobalFila.builder()
                        .fecha(Formatos.fecha(i.getFechaNegocio()))
                        .sucursal(nombres.getOrDefault(String.valueOf(i.getSucursalId()), "-"))
                        .tipo(i.getTipo())
                        .movimiento(movimiento(i))
                        .referenciaId(i.getReferenciaId())
                        .detalle(detalle(i))
                        .clienteNit(i.getClienteNit())
                        .boletos(i.getCantidadBoletos())
                        .monto(i.getMonto())
                        .build())
                .toList();
    }

    private Map<String, Object> fila(IngresoEntidad i) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("fecha", i.getFechaNegocio() != null ? i.getFechaNegocio().toString() : null);
        f.put("tipo", i.getTipo());
        f.put("movimiento", movimiento(i));
        f.put("referenciaId", i.getReferenciaId());
        f.put("detalle", detalle(i));
        f.put("clienteNit", i.getClienteNit());
        f.put("boletos", i.getCantidadBoletos());
        f.put("monto", i.getMonto());
        return f;
    }

    private Map<String, Object> resumenBoletos(List<IngresoEntidad> entidades) {
        int vendidos = entidades.stream()
                .filter(i -> "BOLETO".equals(i.getTipo()))
                .mapToInt(i -> Math.max(i.getCantidadBoletos(), 0))
                .sum();
        int cancelados = entidades.stream()
                .filter(i -> "BOLETO".equals(i.getTipo()))
                .mapToInt(i -> Math.max(-i.getCantidadBoletos(), 0))
                .sum();
        Map<String, Object> resumen = new LinkedHashMap<>();
        resumen.put("boletosVendidos", vendidos);
        resumen.put("boletosCancelados", cancelados);
        resumen.put("boletosNetos", vendidos - cancelados);
        return resumen;
    }

    private String movimiento(IngresoEntidad i) {
        if ("BOLETO".equals(i.getTipo())) {
            return i.getCantidadBoletos() < 0 ? "CANCELACION" : "VENTA";
        }
        return "ALQUILER_PAGADO";
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
