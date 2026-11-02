package gt.cunoc.proyecto2.reporting.reportes.generadores;

import gt.cunoc.proyecto2.reporting.modelos.BusEntidad;
import gt.cunoc.proyecto2.reporting.modelos.IngresoEntidad;
import gt.cunoc.proyecto2.reporting.modelos.ViajeEntidad;
import gt.cunoc.proyecto2.reporting.repositorios.BusRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.IngresoRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.ViajeRepositorio;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Component;

/** Ingresos por alquiler de la sucursal. */
@Component
public class GeneradorIngresosAlquiler implements GeneradorReporte {

    private static final String SIN_BUS = "-";

    private final IngresoRepositorio ingresos;
    private final ViajeRepositorio viajes;
    private final BusRepositorio buses;
    private final CompiladorJasper compilador;

    // Inyecta los repositorios y el compilador de plantillas Jasper.
    public GeneradorIngresosAlquiler(IngresoRepositorio ingresos, ViajeRepositorio viajes,
                                     BusRepositorio buses, CompiladorJasper compilador) {
        this.ingresos = ingresos;
        this.viajes = viajes;
        this.buses = buses;
        this.compilador = compilador;
    }

    @Override
    public TipoReporte tipo() {
        return TipoReporte.INGRESOS_ALQUILER;
    }

    // Genera el PDF del reporte con los datos y los filtros recibidos.
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

    // Devuelve los mismos datos en JSON para la API.
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
            fila.put("busPlaca", f.getBusPlaca());
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

    // Arma las filas del reporte a partir de las consultas con los filtros.
    List<AlquilerFila> filas(ParametrosReporte params) {
        List<IngresoEntidad> cobros = ingresos
                .buscar(params.getSucursalId(), "ALQUILER", params.getDesde(), params.getHasta());
        Map<UUID, String> placas = placasPorAlquiler(cobros);
        return cobros.stream()
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
                        .busPlaca(placaDe(placas, i.getAlquilerId()))
                        .monto(i.getMonto())
                        .build())
                .toList();
    }

    /**
     * La placa de ese alquiler, o "-" si todavía no tiene bus. El id puede venir null (ingresos
     * anteriores a que se guardara el alquiler), y un mapa inmutable no admite ni consultarse con
     * una clave null: por eso se revisa antes en lugar de confiar en getOrDefault.
     */
    private static String placaDe(Map<UUID, String> placas, UUID alquilerId) {
        return alquilerId == null ? SIN_BUS : placas.getOrDefault(alquilerId, SIN_BUS);
    }

    /**
     * Placa del bus de cada alquiler cobrado. El bus no viene en el cobro: se asigna después, con
     * el viaje PRIVADO, así que se resuelve acá en dos consultas (los viajes de esos alquileres y
     * los buses de esos viajes) en lugar de una por fila. Un alquiler sin viaje asignado todavía
     * no tiene bus y se queda sin placa.
     */
    private Map<UUID, String> placasPorAlquiler(List<IngresoEntidad> cobros) {
        Set<UUID> alquileres = cobros.stream()
                .map(IngresoEntidad::getAlquilerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (alquileres.isEmpty()) {
            return Map.of();
        }
        List<ViajeEntidad> deEsosAlquileres = viajes.findByAlquilerIdIn(alquileres);
        Map<UUID, String> placasPorBus = buses
                .findAllById(deEsosAlquileres.stream().map(ViajeEntidad::getBusId)
                        .filter(Objects::nonNull).collect(Collectors.toSet()))
                .stream()
                .filter(b -> b.getPlaca() != null)
                .collect(Collectors.toMap(BusEntidad::getId, BusEntidad::getPlaca, (a, b) -> a));
        return deEsosAlquileres.stream()
                .filter(v -> v.getAlquilerId() != null && placasPorBus.containsKey(v.getBusId()))
                .collect(Collectors.toMap(ViajeEntidad::getAlquilerId,
                        v -> placasPorBus.get(v.getBusId()), (a, b) -> a));
    }
}
