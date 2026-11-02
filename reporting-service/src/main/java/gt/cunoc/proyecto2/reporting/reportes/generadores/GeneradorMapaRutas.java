package gt.cunoc.proyecto2.reporting.reportes.generadores;

import gt.cunoc.proyecto2.reporting.modelos.RutaEntidad;
import gt.cunoc.proyecto2.reporting.modelos.SucursalEntidad;
import gt.cunoc.proyecto2.reporting.repositorios.RutaRepositorio;
import gt.cunoc.proyecto2.reporting.repositorios.SucursalRepositorio;
import gt.cunoc.proyecto2.reporting.reportes.CompiladorJasper;
import gt.cunoc.proyecto2.reporting.reportes.GeneradorReporte;
import gt.cunoc.proyecto2.reporting.reportes.ParametrosReporte;
import gt.cunoc.proyecto2.reporting.reportes.TipoReporte;
import gt.cunoc.proyecto2.reporting.reportes.filas.RutaMapaFila;
import gt.cunoc.proyecto2.reporting.reportes.filas.SucursalMapaFila;
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
 * Sucursales con coordenadas y rutas (Leaflet).
 * El PDF es la tabla de sucursales y rutas (el mapa se imprime desde el navegador).
 */
@Component
public class GeneradorMapaRutas implements GeneradorReporte {

    private final SucursalRepositorio sucursales;
    private final RutaRepositorio rutas;
    private final CompiladorJasper compilador;

    // Inyecta los repositorios y el compilador de plantillas Jasper.
    public GeneradorMapaRutas(SucursalRepositorio sucursales, RutaRepositorio rutas,
                              CompiladorJasper compilador) {
        this.sucursales = sucursales;
        this.rutas = rutas;
        this.compilador = compilador;
    }

    @Override
    public TipoReporte tipo() {
        return TipoReporte.MAPA_RUTAS;
    }

    // Genera el PDF del reporte con los datos y los filtros recibidos.
    @Override
    public byte[] generar(ParametrosReporte params) throws Exception {
        List<SucursalMapaFila> sucursalFilas = sucursalFilas();
        List<RutaMapaFila> rutaFilas = rutaFilas(params);

        JasperReport reporte = compilador.obtener("mapa_rutas");
        Map<String, Object> parametros = new HashMap<>();
        parametros.put("REPORT_TITLE", "Mapa de Rutas por Sucursal");
        parametros.put("RUTAS_REPORTE", compilador.obtener("mapa_rutas_rutas"));
        parametros.put("RUTAS_DATOS", new JRBeanCollectionDataSource(rutaFilas));

        JasperPrint print = JasperFillManager.fillReport(
                reporte, parametros, new JRBeanCollectionDataSource(sucursalFilas));
        return JasperExportManager.exportReportToPdf(print);
    }

    // Devuelve los mismos datos en JSON para la API.
    @Override
    public Object datos(ParametrosReporte params) {
        List<Map<String, Object>> sucursalJson = sucursalFilas().stream().map(s -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("sucursalId", s.getSucursalId());
            fila.put("nombre", s.getNombre());
            fila.put("latitud", s.getLatitud());
            fila.put("longitud", s.getLongitud());
            fila.put("activa", s.getActiva());
            return fila;
        }).toList();
        List<Map<String, Object>> rutaJson = rutaFilas(params).stream().map(r -> {
            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("rutaId", r.getRutaId());
            fila.put("sucursalOrigenId", r.getSucursalOrigenId());
            fila.put("sucursalDestinoId", r.getSucursalDestinoId());
            fila.put("origen", r.getOrigen());
            fila.put("destino", r.getDestino());
            fila.put("distanciaKm", r.getDistanciaKm());
            fila.put("precioBoleto", r.getPrecioBoleto());
            fila.put("activa", r.getActiva());
            return fila;
        }).toList();
        Map<String, Object> resultado = new LinkedHashMap<>();
        resultado.put("sucursales", sucursalJson);
        resultado.put("rutas", rutaJson);
        return resultado;
    }

    List<SucursalMapaFila> sucursalFilas() {
        return sucursales.findAllByOrderByNombreAsc().stream()
                .map(s -> SucursalMapaFila.builder()
                        .sucursalId(s.getId())
                        .nombre(s.getNombre())
                        .latitud(s.getLatitud())
                        .longitud(s.getLongitud())
                        .activa(s.isActiva())
                        .build())
                .toList();
    }

    List<RutaMapaFila> rutaFilas(ParametrosReporte params) {
        List<RutaEntidad> entidades = rutas.findAll();
        if (params.getSucursalId() != null) {
            entidades = entidades.stream()
                    .filter(r -> params.getSucursalId().equals(r.getSucursalOrigenId()))
                    .toList();
        }
        Map<String, String> nombres = new HashMap<>();
        sucursales.findAll().forEach(s -> nombres.put(s.getId().toString(), s.getNombre()));
        return entidades.stream()
                .map(r -> RutaMapaFila.builder()
                        .rutaId(r.getId())
                        .sucursalOrigenId(r.getSucursalOrigenId())
                        .sucursalDestinoId(r.getSucursalDestinoId())
                        .origen(nombres.getOrDefault(String.valueOf(r.getSucursalOrigenId()),
                                r.getOrigenNombre()))
                        .destino(nombres.getOrDefault(String.valueOf(r.getSucursalDestinoId()),
                                r.getDestinoNombre()))
                        .distanciaKm(r.getDistanciaKm())
                        .precioBoleto(r.getPrecioBoleto())
                        .activa(r.isActiva())
                        .build())
                .toList();
    }
}
