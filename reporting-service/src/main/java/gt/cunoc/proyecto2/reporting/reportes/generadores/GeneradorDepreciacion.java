package gt.cunoc.proyecto2.reporting.reportes.generadores;

import gt.cunoc.proyecto2.reporting.modelos.BusEntidad;
import gt.cunoc.proyecto2.reporting.modelos.ViajeEntidad;
import gt.cunoc.proyecto2.reporting.repositorios.BusRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.TarifaDepreciacionRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.ViajeRepositorio;
import gt.cunoc.proyecto2.reporting.reportes.CompiladorJasper;
import gt.cunoc.proyecto2.reporting.reportes.GeneradorReporte;
import gt.cunoc.proyecto2.reporting.reportes.ParametrosReporte;
import gt.cunoc.proyecto2.reporting.reportes.TipoReporte;
import gt.cunoc.proyecto2.reporting.reportes.filas.DepreciacionFila;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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

/**
 * RS5 · Depreciación por bus. Sale de `viajes` en estado COMPLETADO
 * (km, tarifa y costo que mandó Trips).
 */
@Component
public class GeneradorDepreciacion implements GeneradorReporte {

    private final ViajeRepositorio viajes;
    private final BusRepositorio buses;
    private final TarifaDepreciacionRepositorio tarifas;
    private final CompiladorJasper compilador;

    public GeneradorDepreciacion(ViajeRepositorio viajes, BusRepositorio buses,
                                 TarifaDepreciacionRepositorio tarifas,
                                 CompiladorJasper compilador) {
        this.viajes = viajes;
        this.buses = buses;
        this.tarifas = tarifas;
        this.compilador = compilador;
    }

    @Override
    public TipoReporte tipo() {
        return TipoReporte.DEPRECIACION;
    }

    @Override
    public byte[] generar(ParametrosReporte params) throws Exception {
        List<DepreciacionFila> filas = filas(params);
        BigDecimal total = filas.stream().map(DepreciacionFila::getCosto)
                .filter(c -> c != null).reduce(BigDecimal.ZERO, BigDecimal::add);

        JasperReport reporte = compilador.obtener("depreciacion");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("REPORT_TITLE", "Depreciación por Bus");
        parametros.put("FROM_DATE", Formatos.texto(params.getDesde()));
        parametros.put("TO_DATE", Formatos.texto(params.getHasta()));
        parametros.put("TOTAL", Formatos.quetzales(total));
        parametros.put("TARIFA_VIGENTE", textoTarifa(params));

        JasperPrint print = JasperFillManager.fillReport(
                reporte, parametros, new JRBeanCollectionDataSource(filas));
        return JasperExportManager.exportReportToPdf(print);
    }

    @Override
    public Object datos(ParametrosReporte params) {
        List<DepreciacionFila> filas = filas(params);
        Map<String, List<Map<String, Object>>> porBus = new LinkedHashMap<>();
        Map<String, String> placas = new HashMap<>();
        for (DepreciacionFila f : filas) {
            String clave = String.valueOf(f.getBusId());
            placas.putIfAbsent(clave, f.getPlaca());
            porBus.computeIfAbsent(clave, k -> new ArrayList<>()).add(viajeJson(f));
        }
        List<Map<String, Object>> busesJson = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (var grupo : porBus.entrySet()) {
            List<Map<String, Object>> viajesJson = grupo.getValue();
            BigDecimal km = viajesJson.stream().map(v -> (BigDecimal) v.get("kmRecorridos"))
                    .filter(k -> k != null).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal acumulado = viajesJson.stream().map(v -> (BigDecimal) v.get("costo"))
                    .filter(c -> c != null).reduce(BigDecimal.ZERO, BigDecimal::add);
            total = total.add(acumulado);
            Map<String, Object> b = new LinkedHashMap<>();
            b.put("busId", java.util.UUID.fromString(grupo.getKey()));
            b.put("placa", placas.get(grupo.getKey()));
            b.put("kmRecorridos", km);
            b.put("depreciacionAcumulada", acumulado);
            b.put("viajes", viajesJson);
            busesJson.add(b);
        }
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("tarifaVigente", tarifaJson(params));
        resultado.put("buses", busesJson);
        resultado.put("total", total);
        return resultado;
    }

    List<DepreciacionFila> filas(ParametrosReporte params) {
        OffsetDateTime desde = params.getDesde() != null
                ? params.getDesde().atStartOfDay().atOffset(ZoneOffset.UTC) : null;
        OffsetDateTime hasta = params.getHasta() != null
                ? params.getHasta().atTime(23, 59, 59).atOffset(ZoneOffset.UTC) : null;
        Map<String, String> placas = new HashMap<>();
        buses.findAll().forEach(b -> placas.put(b.getId().toString(), b.getPlaca()));
        return viajes.buscar(params.getSucursalId(), params.getBusId(), desde, hasta).stream()
                .filter(v -> "COMPLETADO".equals(v.getEstado()))
                .map(v -> DepreciacionFila.builder()
                        .busId(v.getBusId())
                        .placa(placas.getOrDefault(String.valueOf(v.getBusId()), "-"))
                        .viajeId(v.getViajeId())
                        .fecha(v.getFechaSalida() != null
                                ? v.getFechaSalida().toLocalDate().toString() : "-")
                        .kmRecorridos(v.getKmRecorridos())
                        .tarifa(v.getTarifaDepreciacion())
                        .costo(v.getCostoDepreciacion())
                        .build())
                .toList();
    }

    private String textoTarifa(ParametrosReporte params) {
        return tarifas.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(
                        params.getHasta() != null ? params.getHasta() : LocalDate.now())
                .map(t -> "Q " + t.getMontoPorKm().toPlainString() + " /km desde " + t.getVigenteDesde())
                .orElse("Sin tarifa registrada");
    }

    private Map<String, Object> tarifaJson(ParametrosReporte params) {
        return tarifas.findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(
                        params.getHasta() != null ? params.getHasta() : LocalDate.now())
                .map(t -> {
                    Map<String, Object> tarifa = new LinkedHashMap<>();
                    tarifa.put("montoPorKm", t.getMontoPorKm());
                    tarifa.put("vigenteDesde", t.getVigenteDesde() != null
                            ? t.getVigenteDesde().toString() : null);
                    return tarifa;
                })
                .orElse(null);
    }

    private Map<String, Object> viajeJson(DepreciacionFila f) {
        Map<String, Object> v = new LinkedHashMap<>();
        v.put("viajeId", f.getViajeId());
        v.put("fecha", f.getFecha());
        v.put("kmRecorridos", f.getKmRecorridos());
        v.put("tarifa", f.getTarifa());
        v.put("costo", f.getCosto());
        return v;
    }
}
