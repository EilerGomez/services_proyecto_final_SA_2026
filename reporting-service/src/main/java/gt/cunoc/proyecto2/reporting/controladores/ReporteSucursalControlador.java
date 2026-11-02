package gt.cunoc.proyecto2.reporting.controladores;

import gt.cunoc.proyecto2.comun.errores.NoEncontradoException;
import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.reporting.repositorios.SucursalRepositorio;
import gt.cunoc.proyecto2.reporting.reportes.FabricaGeneradores;
import gt.cunoc.proyecto2.reporting.reportes.ParametrosReporte;
import gt.cunoc.proyecto2.reporting.reportes.TipoReporte;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Expone los reportes de una sucursal concreta.
 *
 * <p>El administrador de sucursal solo puede consultar la suya, el administrador del sistema puede
 * consultar cualquiera. Si la sucursal no existe en la proyección de reportes responde 404, y si es
 * ajena al usuario responde 403. Cada reporte se devuelve en JSON o PDF.
 */
@RestController
@RequestMapping("/reports/sucursales/{id}")
@Tag(name = "Reportes por sucursal", description = "Buses, choferes, ingresos, depreciación y taller de una sucursal")
@RequiereRol({Roles.ADMIN_SUCURSAL, Roles.ADMIN_SISTEMA})
public class ReporteSucursalControlador {

    private final FabricaGeneradores fabrica;
    private final SucursalRepositorio sucursales;

    // Inyecta la fábrica de reportes y el repositorio que confirma que la sucursal existe.
    public ReporteSucursalControlador(FabricaGeneradores fabrica, SucursalRepositorio sucursales) {
        this.fabrica = fabrica;
        this.sucursales = sucursales;
    }

    // Comprueba que la sucursal exista en los reportes y que el usuario tenga acceso a ella.
    private void verificarAcceso(UUID id, UsuarioActual usuario) {
        if (!sucursales.existsById(id)) {
            throw new NoEncontradoException("SUCURSAL_NO_EXISTE", "La sucursal no existe en los reportes");
        }
        usuario.verificarAccesoSucursal(id);
    }

    // Lista los buses de la sucursal, con filtro opcional por estado.
    @GetMapping("/buses")
    @Operation(summary = "Buses de la sucursal (json | pdf)")
    public ResponseEntity<?> buses(@PathVariable UUID id,
                                   @RequestParam(required = false) String estado,
                                   @Parameter(description = "json o pdf")
                                   @RequestParam(defaultValue = "pdf") String format,
                                   @Parameter(hidden = true) UsuarioActual usuario) throws Exception {
        verificarAcceso(id, usuario);
        ParametrosReporte params =
                ParametrosReporte.builder().sucursalId(id).estado(estado).build();
        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(fabrica.obtener(TipoReporte.BUSES).datos(params));
        }
        return ReporteSistemaControlador.pdf(
                fabrica.obtener(TipoReporte.BUSES).generar(params), "buses", null, null);
    }

    // Lista los choferes de la sucursal, con filtro opcional por estado.
    @GetMapping("/choferes")
    @Operation(summary = "Choferes de la sucursal (json | pdf)")
    public ResponseEntity<?> choferes(@PathVariable UUID id,
                                      @RequestParam(required = false) String estado,
                                      @Parameter(description = "json o pdf")
                                      @RequestParam(defaultValue = "pdf") String format,
                                      @Parameter(hidden = true) UsuarioActual usuario) throws Exception {
        verificarAcceso(id, usuario);
        ParametrosReporte params =
                ParametrosReporte.builder().sucursalId(id).estado(estado).build();
        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(fabrica.obtener(TipoReporte.CHOFERES).datos(params));
        }
        return ReporteSistemaControlador.pdf(
                fabrica.obtener(TipoReporte.CHOFERES).generar(params), "choferes", null, null);
    }

    // Ingresos por boletos de la sucursal, filtrables por ruta, bus y rango de fechas.
    @GetMapping("/ingresos-boletos")
    @Operation(summary = "Ingresos por boletos, filtros ruta y bus (json | pdf)")
    public ResponseEntity<?> ingresosBoletos(@PathVariable UUID id,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                             @RequestParam(required = false) UUID rutaId,
                                             @RequestParam(required = false) UUID busId,
                                             @Parameter(description = "json o pdf")
                                             @RequestParam(defaultValue = "pdf") String format,
                                             @Parameter(hidden = true) UsuarioActual usuario) throws Exception {
        verificarAcceso(id, usuario);
        ParametrosReporte params = ParametrosReporte.builder()
                .sucursalId(id).desde(desde).hasta(hasta).rutaId(rutaId).busId(busId).build();
        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(fabrica.obtener(TipoReporte.INGRESOS_BOLETOS).datos(params));
        }
        return ReporteSistemaControlador.pdf(
                fabrica.obtener(TipoReporte.INGRESOS_BOLETOS).generar(params),
                "ingresos-boletos", desde, hasta);
    }

    // Ingresos por alquiler de vehículos con conductor en el rango de fechas.
    @GetMapping("/ingresos-alquiler")
    @Operation(summary = "Ingresos por alquiler (json | pdf)")
    public ResponseEntity<?> ingresosAlquiler(@PathVariable UUID id,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                              @Parameter(description = "json o pdf")
                                              @RequestParam(defaultValue = "pdf") String format,
                                              @Parameter(hidden = true) UsuarioActual usuario) throws Exception {
        verificarAcceso(id, usuario);
        ParametrosReporte params = ParametrosReporte.builder()
                .sucursalId(id).desde(desde).hasta(hasta).build();
        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(fabrica.obtener(TipoReporte.INGRESOS_ALQUILER).datos(params));
        }
        return ReporteSistemaControlador.pdf(
                fabrica.obtener(TipoReporte.INGRESOS_ALQUILER).generar(params),
                "ingresos-alquiler", desde, hasta);
    }

    // Depreciación calculada por bus, con filtro opcional de bus y fechas.
    @GetMapping("/depreciacion")
    @Operation(summary = "Depreciación por bus (json | pdf)")
    public ResponseEntity<?> depreciacion(@PathVariable UUID id,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                          @RequestParam(required = false) UUID busId,
                                          @Parameter(description = "json o pdf")
                                          @RequestParam(defaultValue = "pdf") String format,
                                          @Parameter(hidden = true) UsuarioActual usuario) throws Exception {
        verificarAcceso(id, usuario);
        ParametrosReporte params = ParametrosReporte.builder()
                .sucursalId(id).desde(desde).hasta(hasta).busId(busId).build();
        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(fabrica.obtener(TipoReporte.DEPRECIACION).datos(params));
        }
        return ReporteSistemaControlador.pdf(
                fabrica.obtener(TipoReporte.DEPRECIACION).generar(params),
                "depreciacion", desde, hasta);
    }

    // Gastos de taller por bus, agrupados por categoría de gasto.
    @GetMapping("/taller-por-bus")
    @Operation(summary = "Gastos de taller por bus, agrupados por categoría (json | pdf)")
    public ResponseEntity<?> tallerPorBus(@PathVariable UUID id,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                          @RequestParam(required = false) UUID busId,
                                          @RequestParam(required = false) UUID categoriaId,
                                          @Parameter(description = "json o pdf")
                                          @RequestParam(defaultValue = "pdf") String format,
                                          @Parameter(hidden = true) UsuarioActual usuario) throws Exception {
        verificarAcceso(id, usuario);
        ParametrosReporte params = ParametrosReporte.builder()
                .sucursalId(id).desde(desde).hasta(hasta).busId(busId).categoriaId(categoriaId).build();
        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(fabrica.obtener(TipoReporte.TALLER_POR_BUS).datos(params));
        }
        return ReporteSistemaControlador.pdf(
                fabrica.obtener(TipoReporte.TALLER_POR_BUS).generar(params),
                "taller-por-bus", desde, hasta);
    }

    // Variante del reporte anterior que muestra, por bus, el subtotal de cada categoría.
    @GetMapping("/taller-por-bus-categoria")
    @Operation(summary = "Por cada bus, el subtotal de cada categoría de gasto (json | pdf)")
    public ResponseEntity<?> tallerPorBusCategoria(@PathVariable UUID id,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                          @RequestParam(required = false) UUID busId,
                                          @RequestParam(required = false) UUID categoriaId,
                                          @Parameter(description = "json o pdf")
                                          @RequestParam(defaultValue = "pdf") String format,
                                          @Parameter(hidden = true) UsuarioActual usuario) throws Exception {
        verificarAcceso(id, usuario);
        ParametrosReporte params = ParametrosReporte.builder()
                .sucursalId(id).desde(desde).hasta(hasta).busId(busId).categoriaId(categoriaId).build();
        if ("json".equalsIgnoreCase(format)) {
            return ResponseEntity.ok(fabrica.obtener(TipoReporte.TALLER_BUS_CATEGORIA).datos(params));
        }
        return ReporteSistemaControlador.pdf(
                fabrica.obtener(TipoReporte.TALLER_BUS_CATEGORIA).generar(params),
                "taller-por-bus-categoria", desde, hasta);
    }
}
