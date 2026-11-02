package gt.cunoc.proyecto2.reporting.controladores;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.reporting.reportes.FabricaGeneradores;
import gt.cunoc.proyecto2.reporting.reportes.ParametrosReporte;
import gt.cunoc.proyecto2.reporting.reportes.TipoReporte;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone los reportes globales del sistema.
 *
 * <p>Todos los endpoints son de uso exclusivo del administrador del sistema y responden JSON o PDF
 * según format=json|pdf, si no se indica, devuelven PDF. El archivo PDF se descarga con un
 * nombre que incluye el rango de fechas solicitado.
 */
@RestController
@RequestMapping("/reports")
@Tag(name = "Reportes del sistema", description = "Reportes globales de ingresos, gastos, ganancias, rutas y taller")
@RequiereRol(Roles.ADMIN_SISTEMA)
public class ReporteSistemaControlador {

    private final FabricaGeneradores fabrica;

    // Inyecta la fábrica que resuelve el generador de cada tipo de reporte.
    public ReporteSistemaControlador(FabricaGeneradores fabrica) {
        this.fabrica = fabrica;
    }

    // Ingresos por sucursal en el rango de fechas, permite limitar a una sucursal.
    @GetMapping("/ingresos")
    @Operation(summary = "Ingresos por sucursal (json | pdf)")
    public ResponseEntity<?> ingresos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sucursalId,
            @Parameter(description = "json o pdf") @RequestParam(defaultValue = "pdf") String format)
            throws Exception {
        ParametrosReporte params = ParametrosReporte.builder()
                .desde(desde).hasta(hasta).sucursalId(sucursalId).build();
        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(fabrica.obtener(TipoReporte.INGRESOS).datos(params));
        }
        return pdf(fabrica.obtener(TipoReporte.INGRESOS).generar(params), "ingresos", desde, hasta);
    }

    // Gastos por sucursal y tipo de gasto en el rango de fechas.
    @GetMapping("/gastos")
    @Operation(summary = "Gastos por sucursal y tipo (json | pdf)")
    public ResponseEntity<?> gastos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sucursalId,
            @Parameter(description = "json o pdf") @RequestParam(defaultValue = "pdf") String format)
            throws Exception {
        ParametrosReporte params = ParametrosReporte.builder()
                .desde(desde).hasta(hasta).sucursalId(sucursalId).build();
        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(fabrica.obtener(TipoReporte.GASTOS).datos(params));
        }
        return pdf(fabrica.obtener(TipoReporte.GASTOS).generar(params), "gastos", desde, hasta);
    }

    // Ganancias por sucursal, calculadas como ingresos menos gastos.
    @GetMapping("/ganancias")
    @Operation(summary = "Ganancias por sucursal (json | pdf)")
    public ResponseEntity<?> ganancias(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sucursalId,
            @Parameter(description = "json o pdf") @RequestParam(defaultValue = "pdf") String format)
            throws Exception {
        ParametrosReporte params = ParametrosReporte.builder()
                .desde(desde).hasta(hasta).sucursalId(sucursalId).build();
        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(fabrica.obtener(TipoReporte.GANANCIAS).datos(params));
        }
        return pdf(fabrica.obtener(TipoReporte.GANANCIAS).generar(params), "ganancias", desde, hasta);
    }

    // Rutas con más boletos vendidos; el límite controla cuántas se devuelven.
    @GetMapping("/rutas-demandadas")
    @Operation(summary = "Rutas más demandadas por boletos (json | pdf)")
    public ResponseEntity<?> rutasDemandadas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Filtra por sucursal; si se omite, incluye todas")
            @RequestParam(required = false) UUID sucursalId,
            @RequestParam(defaultValue = "10") int limite,
            @Parameter(description = "json o pdf") @RequestParam(defaultValue = "pdf") String format)
            throws Exception {
        ParametrosReporte params = ParametrosReporte.builder()
                .desde(desde).hasta(hasta).sucursalId(sucursalId).limite(limite).build();
        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(fabrica.obtener(TipoReporte.RUTAS_DEMANDADAS).datos(params));
        }
        return pdf(fabrica.obtener(TipoReporte.RUTAS_DEMANDADAS).generar(params),
                "rutas-demandadas", desde, hasta);
    }

    // Gastos de taller agrupados por categoría, con filtro opcional de sucursal y bus.
    @GetMapping("/taller-por-categoria")
    @Operation(summary = "Gastos de taller por categoría (json | pdf)")
    public ResponseEntity<?> tallerPorCategoria(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sucursalId,
            @RequestParam(required = false) UUID busId,
            @Parameter(description = "json o pdf") @RequestParam(defaultValue = "pdf") String format)
            throws Exception {
        ParametrosReporte params = ParametrosReporte.builder()
                .desde(desde).hasta(hasta).sucursalId(sucursalId).busId(busId).build();
        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(fabrica.obtener(TipoReporte.TALLER_POR_CATEGORIA).datos(params));
        }
        return pdf(fabrica.obtener(TipoReporte.TALLER_POR_CATEGORIA).generar(params),
                "taller-por-categoria", desde, hasta);
    }

    // Mapa de sucursales y rutas: JSON para pintarlo con Leaflet o PDF como tabla de respaldo.
    @GetMapping("/mapa-rutas")
    @Operation(summary = "Sucursales con coordenadas y rutas, Leaflet (json) o tabla (pdf)")
    public ResponseEntity<?> mapaRutas(
            @RequestParam(required = false) UUID sucursalId,
            @Parameter(description = "json o pdf") @RequestParam(defaultValue = "pdf") String format)
            throws Exception {
        ParametrosReporte params = ParametrosReporte.builder().sucursalId(sucursalId).build();
        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(fabrica.obtener(TipoReporte.MAPA_RUTAS).datos(params));
        }
        return pdf(fabrica.obtener(TipoReporte.MAPA_RUTAS).generar(params),
                "mapa-rutas", null, null);
    }

    // Envuelve el PDF en la respuesta de descarga y arma el nombre del archivo con el rango de fechas.
    static ResponseEntity<byte[]> pdf(byte[] contenido, String reporte, LocalDate desde, LocalDate hasta) {
        String archivo = reporte + "_" + (desde != null ? desde : "todos")
                + "_" + (hasta != null ? hasta : "todos") + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + archivo + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(contenido.length)
                .body(contenido);
    }
}
