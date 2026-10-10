package gt.cunoc.proyecto2.reporting.controladores;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.reporting.reportes.FabricaGeneradores;
import gt.cunoc.proyecto2.reporting.reportes.ParametrosReporte;
import gt.cunoc.proyecto2.reporting.reportes.TipoReporte;
import io.swagger.v3.oas.annotations.Operation;
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

@RestController
@RequestMapping("/reports")
@Tag(name = "Reportes del sistema", description = "RG1–RG6: globales (ADMIN_SISTEMA)")
@RequiereRol(Roles.ADMIN_SISTEMA)
public class ReporteSistemaControlador {

    private final FabricaGeneradores fabrica;

    public ReporteSistemaControlador(FabricaGeneradores fabrica) {
        this.fabrica = fabrica;
    }

    @GetMapping("/ingresos")
    @Operation(summary = "RG1 · Ingresos por sucursal")
    public ResponseEntity<?> ingresos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sucursalId) {
        return ResponseEntity.ok(fabrica.obtener(TipoReporte.INGRESOS)
                .datos(ParametrosReporte.builder().desde(desde).hasta(hasta).sucursalId(sucursalId).build()));
    }

    @GetMapping("/ingresos/pdf")
    @Operation(summary = "RG1 · Ingresos por sucursal (PDF)")
    public ResponseEntity<byte[]> ingresosPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sucursalId) throws Exception {
        return pdf(fabrica.obtener(TipoReporte.INGRESOS)
                .generar(ParametrosReporte.builder().desde(desde).hasta(hasta).sucursalId(sucursalId).build()),
                "ingresos", desde, hasta);
    }

    @GetMapping("/gastos")
    @Operation(summary = "RG2 · Gastos por sucursal y tipo")
    public ResponseEntity<?> gastos(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sucursalId) {
        return ResponseEntity.ok(fabrica.obtener(TipoReporte.GASTOS)
                .datos(ParametrosReporte.builder().desde(desde).hasta(hasta).sucursalId(sucursalId).build()));
    }

    @GetMapping("/gastos/pdf")
    @Operation(summary = "RG2 · Gastos por sucursal y tipo (PDF)")
    public ResponseEntity<byte[]> gastosPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sucursalId) throws Exception {
        return pdf(fabrica.obtener(TipoReporte.GASTOS)
                .generar(ParametrosReporte.builder().desde(desde).hasta(hasta).sucursalId(sucursalId).build()),
                "gastos", desde, hasta);
    }

    @GetMapping("/ganancias")
    @Operation(summary = "RG3 · Ganancias por sucursal")
    public ResponseEntity<?> ganancias(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sucursalId) {
        return ResponseEntity.ok(fabrica.obtener(TipoReporte.GANANCIAS)
                .datos(ParametrosReporte.builder().desde(desde).hasta(hasta).sucursalId(sucursalId).build()));
    }

    @GetMapping("/ganancias/pdf")
    @Operation(summary = "RG3 · Ganancias por sucursal (PDF)")
    public ResponseEntity<byte[]> gananciasPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sucursalId) throws Exception {
        return pdf(fabrica.obtener(TipoReporte.GANANCIAS)
                .generar(ParametrosReporte.builder().desde(desde).hasta(hasta).sucursalId(sucursalId).build()),
                "ganancias", desde, hasta);
    }

    @GetMapping("/rutas-demandadas")
    @Operation(summary = "RG4 · Rutas más demandadas por boletos")
    public ResponseEntity<?> rutasDemandadas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "10") int limite) {
        return ResponseEntity.ok(fabrica.obtener(TipoReporte.RUTAS_DEMANDADAS)
                .datos(ParametrosReporte.builder().desde(desde).hasta(hasta).limite(limite).build()));
    }

    @GetMapping("/rutas-demandadas/pdf")
    @Operation(summary = "RG4 · Rutas más demandadas por boletos (PDF)")
    public ResponseEntity<byte[]> rutasDemandadasPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "10") int limite) throws Exception {
        return pdf(fabrica.obtener(TipoReporte.RUTAS_DEMANDADAS)
                .generar(ParametrosReporte.builder().desde(desde).hasta(hasta).limite(limite).build()),
                "rutas-demandadas", desde, hasta);
    }

    @GetMapping("/taller-por-categoria")
    @Operation(summary = "RG5 · Gastos de taller por categoría")
    public ResponseEntity<?> tallerPorCategoria(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sucursalId,
            @RequestParam(required = false) UUID busId) {
        return ResponseEntity.ok(fabrica.obtener(TipoReporte.TALLER_POR_CATEGORIA).datos(
                ParametrosReporte.builder().desde(desde).hasta(hasta).sucursalId(sucursalId).busId(busId).build()));
    }

    @GetMapping("/taller-por-categoria/pdf")
    @Operation(summary = "RG5 · Gastos de taller por categoría (PDF)")
    public ResponseEntity<byte[]> tallerPorCategoriaPdf(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID sucursalId,
            @RequestParam(required = false) UUID busId) throws Exception {
        return pdf(fabrica.obtener(TipoReporte.TALLER_POR_CATEGORIA).generar(
                ParametrosReporte.builder().desde(desde).hasta(hasta).sucursalId(sucursalId).busId(busId).build()),
                "taller-por-categoria", desde, hasta);
    }

    @GetMapping("/mapa-rutas")
    @Operation(summary = "RG6 · Sucursales con coordenadas y rutas (Leaflet)")
    public ResponseEntity<?> mapaRutas(@RequestParam(required = false) UUID sucursalId) {
        return ResponseEntity.ok(fabrica.obtener(TipoReporte.MAPA_RUTAS)
                .datos(ParametrosReporte.builder().sucursalId(sucursalId).build()));
    }

    @GetMapping("/mapa-rutas/pdf")
    @Operation(summary = "RG6 · Tabla de sucursales y rutas (PDF)")
    public ResponseEntity<byte[]> mapaRutasPdf(@RequestParam(required = false) UUID sucursalId)
            throws Exception {
        return pdf(fabrica.obtener(TipoReporte.MAPA_RUTAS)
                .generar(ParametrosReporte.builder().sucursalId(sucursalId).build()),
                "mapa-rutas", null, null);
    }

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
