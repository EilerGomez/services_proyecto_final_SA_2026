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

@RestController
@RequestMapping("/reports/sucursales/{id}")
@Tag(name = "Reportes por sucursal", description = "RS1–RS6 (ADMIN_SUCURSAL su sucursal, ADMIN_SISTEMA cualquiera)")
@RequiereRol({Roles.ADMIN_SUCURSAL, Roles.ADMIN_SISTEMA})
public class ReporteSucursalControlador {

    private final FabricaGeneradores fabrica;
    private final SucursalRepositorio sucursales;

    public ReporteSucursalControlador(FabricaGeneradores fabrica, SucursalRepositorio sucursales) {
        this.fabrica = fabrica;
        this.sucursales = sucursales;
    }

    /** 404 si la sucursal no está en la proyección + 403 si es ajena al usuario. */
    private void verificarAcceso(UUID id, UsuarioActual usuario) {
        if (!sucursales.existsById(id)) {
            throw new NoEncontradoException("SUCURSAL_NO_EXISTE", "La sucursal no existe en los reportes");
        }
        usuario.verificarAccesoSucursal(id);
    }

    @GetMapping("/buses")
    @Operation(summary = "RS1 · Buses de la sucursal")
    public ResponseEntity<?> buses(@PathVariable UUID id,
                                   @RequestParam(required = false) String estado,
                                   @Parameter(hidden = true) UsuarioActual usuario) {
        verificarAcceso(id, usuario);
        return ResponseEntity.ok(fabrica.obtener(TipoReporte.BUSES).datos(
                ParametrosReporte.builder().sucursalId(id).estado(estado).build()));
    }

    @GetMapping("/buses/pdf")
    @Operation(summary = "RS1 · Buses de la sucursal (PDF)")
    public ResponseEntity<byte[]> busesPdf(@PathVariable UUID id,
                                           @RequestParam(required = false) String estado,
                                           @Parameter(hidden = true) UsuarioActual usuario) throws Exception {
        verificarAcceso(id, usuario);
        return ReporteSistemaControlador.pdf(fabrica.obtener(TipoReporte.BUSES).generar(
                ParametrosReporte.builder().sucursalId(id).estado(estado).build()), "buses", null, null);
    }

    @GetMapping("/choferes")
    @Operation(summary = "RS2 · Choferes de la sucursal")
    public ResponseEntity<?> choferes(@PathVariable UUID id,
                                      @RequestParam(required = false) String estado,
                                      @Parameter(hidden = true) UsuarioActual usuario) {
        verificarAcceso(id, usuario);
        return ResponseEntity.ok(fabrica.obtener(TipoReporte.CHOFERES).datos(
                ParametrosReporte.builder().sucursalId(id).estado(estado).build()));
    }

    @GetMapping("/choferes/pdf")
    @Operation(summary = "RS2 · Choferes de la sucursal (PDF)")
    public ResponseEntity<byte[]> choferesPdf(@PathVariable UUID id,
                                              @RequestParam(required = false) String estado,
                                              @Parameter(hidden = true) UsuarioActual usuario) throws Exception {
        verificarAcceso(id, usuario);
        return ReporteSistemaControlador.pdf(fabrica.obtener(TipoReporte.CHOFERES).generar(
                ParametrosReporte.builder().sucursalId(id).estado(estado).build()), "choferes", null, null);
    }

    @GetMapping("/ingresos-boletos")
    @Operation(summary = "RS3 · Ingresos por boletos (filtros ruta y bus)")
    public ResponseEntity<?> ingresosBoletos(@PathVariable UUID id,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                             @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                             @RequestParam(required = false) UUID rutaId,
                                             @RequestParam(required = false) UUID busId,
                                             @Parameter(hidden = true) UsuarioActual usuario) {
        verificarAcceso(id, usuario);
        return ResponseEntity.ok(fabrica.obtener(TipoReporte.INGRESOS_BOLETOS).datos(
                ParametrosReporte.builder().sucursalId(id).desde(desde).hasta(hasta).rutaId(rutaId).busId(busId).build()));
    }

    @GetMapping("/ingresos-boletos/pdf")
    @Operation(summary = "RS3 · Ingresos por boletos (PDF)")
    public ResponseEntity<byte[]> ingresosBoletosPdf(@PathVariable UUID id,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                     @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                                     @RequestParam(required = false) UUID rutaId,
                                                     @RequestParam(required = false) UUID busId,
                                                     @Parameter(hidden = true) UsuarioActual usuario) throws Exception {
        verificarAcceso(id, usuario);
        return ReporteSistemaControlador.pdf(fabrica.obtener(TipoReporte.INGRESOS_BOLETOS).generar(
                ParametrosReporte.builder().sucursalId(id).desde(desde).hasta(hasta).rutaId(rutaId).busId(busId).build()),
                "ingresos-boletos", desde, hasta);
    }

    @GetMapping("/ingresos-alquiler")
    @Operation(summary = "RS4 · Ingresos por alquiler")
    public ResponseEntity<?> ingresosAlquiler(@PathVariable UUID id,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                              @Parameter(hidden = true) UsuarioActual usuario) {
        verificarAcceso(id, usuario);
        return ResponseEntity.ok(fabrica.obtener(TipoReporte.INGRESOS_ALQUILER).datos(
                ParametrosReporte.builder().sucursalId(id).desde(desde).hasta(hasta).build()));
    }

    @GetMapping("/ingresos-alquiler/pdf")
    @Operation(summary = "RS4 · Ingresos por alquiler (PDF)")
    public ResponseEntity<byte[]> ingresosAlquilerPdf(@PathVariable UUID id,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                                      @Parameter(hidden = true) UsuarioActual usuario) throws Exception {
        verificarAcceso(id, usuario);
        return ReporteSistemaControlador.pdf(fabrica.obtener(TipoReporte.INGRESOS_ALQUILER).generar(
                ParametrosReporte.builder().sucursalId(id).desde(desde).hasta(hasta).build()),
                "ingresos-alquiler", desde, hasta);
    }

    @GetMapping("/depreciacion")
    @Operation(summary = "RS5 · Depreciación por bus")
    public ResponseEntity<?> depreciacion(@PathVariable UUID id,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                          @RequestParam(required = false) UUID busId,
                                          @Parameter(hidden = true) UsuarioActual usuario) {
        verificarAcceso(id, usuario);
        return ResponseEntity.ok(fabrica.obtener(TipoReporte.DEPRECIACION).datos(
                ParametrosReporte.builder().sucursalId(id).desde(desde).hasta(hasta).busId(busId).build()));
    }

    @GetMapping("/depreciacion/pdf")
    @Operation(summary = "RS5 · Depreciación por bus (PDF)")
    public ResponseEntity<byte[]> depreciacionPdf(@PathVariable UUID id,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                                  @RequestParam(required = false) UUID busId,
                                                  @Parameter(hidden = true) UsuarioActual usuario) throws Exception {
        verificarAcceso(id, usuario);
        return ReporteSistemaControlador.pdf(fabrica.obtener(TipoReporte.DEPRECIACION).generar(
                ParametrosReporte.builder().sucursalId(id).desde(desde).hasta(hasta).busId(busId).build()),
                "depreciacion", desde, hasta);
    }

    @GetMapping("/taller-por-bus")
    @Operation(summary = "RS6 · Gastos de taller por bus, agrupados por categoría")
    public ResponseEntity<?> tallerPorBus(@PathVariable UUID id,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                          @RequestParam(required = false) UUID busId,
                                          @Parameter(hidden = true) UsuarioActual usuario) {
        verificarAcceso(id, usuario);
        return ResponseEntity.ok(fabrica.obtener(TipoReporte.TALLER_POR_BUS).datos(
                ParametrosReporte.builder().sucursalId(id).desde(desde).hasta(hasta).busId(busId).build()));
    }

    @GetMapping("/taller-por-bus/pdf")
    @Operation(summary = "RS6 · Gastos de taller por bus (PDF)")
    public ResponseEntity<byte[]> tallerPorBusPdf(@PathVariable UUID id,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                                                  @RequestParam(required = false) UUID busId,
                                                  @Parameter(hidden = true) UsuarioActual usuario) throws Exception {
        verificarAcceso(id, usuario);
        return ReporteSistemaControlador.pdf(fabrica.obtener(TipoReporte.TALLER_POR_BUS).generar(
                ParametrosReporte.builder().sucursalId(id).desde(desde).hasta(hasta).busId(busId).build()),
                "taller-por-bus", desde, hasta);
    }
}
