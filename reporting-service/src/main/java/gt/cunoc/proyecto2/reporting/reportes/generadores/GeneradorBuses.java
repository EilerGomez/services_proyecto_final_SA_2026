package gt.cunoc.proyecto2.reporting.reportes.generadores;

import gt.cunoc.proyecto2.reporting.modelos.BusEntidad;
import gt.cunoc.proyecto2.reporting.repositorios.BusRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.ChoferRepositorio;
import gt.cunoc.proyecto2.reporting.reportes.CompiladorJasper;
import gt.cunoc.proyecto2.reporting.reportes.GeneradorReporte;
import gt.cunoc.proyecto2.reporting.reportes.ParametrosReporte;
import gt.cunoc.proyecto2.reporting.reportes.TipoReporte;
import gt.cunoc.proyecto2.reporting.reportes.filas.BusFila;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import org.springframework.stereotype.Component;

/** Listado de buses de la sucursal. */
@Component
public class GeneradorBuses implements GeneradorReporte {

    private final BusRepositorio buses;
    private final ChoferRepositorio choferes;
    private final CompiladorJasper compilador;

    // Inyecta los repositorios y el compilador de plantillas Jasper.
    public GeneradorBuses(BusRepositorio buses, ChoferRepositorio choferes,
                          CompiladorJasper compilador) {
        this.buses = buses;
        this.choferes = choferes;
        this.compilador = compilador;
    }

    @Override
    public TipoReporte tipo() {
        return TipoReporte.BUSES;
    }

    // Genera el PDF del reporte con los datos y los filtros recibidos.
    @Override
    public byte[] generar(ParametrosReporte params) throws Exception {
        List<BusFila> filas = filas(params);

        JasperReport reporte = compilador.obtener("buses");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("REPORT_TITLE", "Buses de la Sucursal");
        parametros.put("TOTAL_BUSES", String.valueOf(filas.size()));

        JasperPrint print = JasperFillManager.fillReport(
                reporte, parametros, new JRBeanCollectionDataSource(filas));
        return JasperExportManager.exportReportToPdf(print);
    }

    // Devuelve los mismos datos en JSON para la API.
    @Override
    public Object datos(ParametrosReporte params) {
        return filas(params).stream().map(f -> {
            Map<String, Object> fila = new java.util.LinkedHashMap<>();
            fila.put("busId", f.getBusId());
            fila.put("placa", f.getPlaca());
            fila.put("marca", f.getMarca());
            fila.put("modelo", f.getModelo());
            fila.put("capacidadPasajeros", f.getCapacidadPasajeros());
            fila.put("estado", f.getEstado());
            fila.put("choferActual", f.getChoferActual());
            fila.put("kilometrajeActual", f.getKilometrajeActual());
            fila.put("totalViajes", f.getTotalViajes());
            return fila;
        }).toList();
    }

    // Arma las filas del reporte a partir de las consultas con los filtros.
    List<BusFila> filas(ParametrosReporte params) {
        Map<String, String> nombres = new HashMap<>();
        choferes.findAll().forEach(c -> nombres.put(c.getId().toString(), c.getNombreCompleto()));
        return buses.findBySucursalIdOrderByPlacaAsc(params.getSucursalId()).stream()
                .filter(b -> params.getEstado() == null || params.getEstado().equals(b.getEstado()))
                .map(b -> BusFila.builder()
                        .busId(b.getId())
                        .placa(b.getPlaca())
                        .marca(b.getMarca())
                        .modelo(b.getModelo())
                        .capacidadPasajeros(b.getCapacidadPasajeros())
                        .estado(b.getEstado())
                        .choferActual(choferActual(b, nombres))
                        .kilometrajeActual(b.getKilometrajeActual())
                        .totalViajes(b.getTotalViajes())
                        .build())
                .toList();
    }

    private String choferActual(BusEntidad bus, Map<String, String> nombres) {
        if (bus.getChoferActualId() == null) {
            return "-";
        }
        return nombres.getOrDefault(bus.getChoferActualId().toString(), "-");
    }
}
