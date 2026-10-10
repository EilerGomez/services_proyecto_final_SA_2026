package gt.cunoc.proyecto2.sales.controladores.ventas;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.dtos.ventas.FiltroVentas;
import gt.cunoc.proyecto2.sales.dtos.ventas.NuevaVentaMostradorSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.VentaRespuesta;
import gt.cunoc.proyecto2.sales.modelos.ventas.CanalVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.EstadoVenta;
import gt.cunoc.proyecto2.sales.servicios.ventas.VentaServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sales/ventas")
@Tag(name = "Ventas", description = "Venta de boletos en mostrador y en línea, consulta y listado")
public class VentaControlador {

    private final VentaServicio servicio;

    public VentaControlador(VentaServicio servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/mostrador")
    @RequiereRol({Roles.CAJERO, Roles.ADMIN_SUCURSAL})
    @Operation(summary = "S4 · Venta en mostrador (efectivo, confirmada al instante)")
    public ResponseEntity<VentaRespuesta> venderEnMostrador(@RequestBody @Valid NuevaVentaMostradorSolicitud solicitud,
                                                            @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.venderEnMostrador(solicitud, usuario));
    }

    @GetMapping("/{id}")
    @RequiereRol({Roles.CLIENTE, Roles.CAJERO, Roles.ADMIN_SUCURSAL, Roles.ADMIN_SISTEMA})
    @Operation(summary = "S3 · Detalle y estado de una venta (el frontend la consulta mientras se paga)")
    public ResponseEntity<VentaRespuesta> obtener(@PathVariable UUID id,
                                                  @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.obtener(id, usuario));
    }

    @GetMapping
    @RequiereRol({Roles.CAJERO, Roles.ADMIN_SUCURSAL, Roles.ADMIN_SISTEMA})
    @Operation(summary = "S6 · Ventas de la sucursal con filtros (fechas de pago)")
    public ResponseEntity<List<VentaRespuesta>> listar(
            @RequestParam(required = false) UUID viajeId,
            @Parameter(description = "Solo ADMIN_SISTEMA puede pedir otra sucursal")
            @RequestParam(required = false) UUID sucursalId,
            @RequestParam(required = false) EstadoVenta estado,
            @RequestParam(required = false) CanalVenta canal,
            @Parameter(description = "Fecha de pago desde (yyyy-MM-dd)") @RequestParam(required = false) LocalDate desde,
            @Parameter(description = "Fecha de pago hasta (yyyy-MM-dd)") @RequestParam(required = false) LocalDate hasta,
            @Parameter(hidden = true) UsuarioActual usuario) {
        FiltroVentas filtro = new FiltroVentas(viajeId, sucursalId, estado, canal, desde, hasta).paraUsuario(usuario);
        return ResponseEntity.ok(servicio.listar(filtro));
    }
}