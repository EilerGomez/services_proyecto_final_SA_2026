package gt.cunoc.proyecto2.fleet.controladores.sucursales;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.ActualizarSucursalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.CambiarEstadoSucursalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.NuevaSucursalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.SucursalCreadaRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.SucursalRespuesta;
import gt.cunoc.proyecto2.fleet.servicios.sucursales.SucursalServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/fleet/sucursales")
@Tag(name = "Sucursales", description = "Alta, edición y estado de las sucursales")
public class SucursalControlador {

    private final SucursalServicio servicio;

    public SucursalControlador(SucursalServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "F1 · Lista las sucursales (público)")
    public ResponseEntity<List<SucursalRespuesta>> listar(
            @Parameter(description = "true = activas, false = inactivas, vacío = todas")
            @RequestParam(required = false) Boolean activa) {
        List<SucursalRespuesta> sucursales = servicio.listar(activa).stream()
                .map(SucursalRespuesta::new)
                .toList();
        return ResponseEntity.ok(sucursales);
    }

    @GetMapping("/{id}")
    @Operation(summary = "F2 · Detalle de una sucursal (público)")
    public ResponseEntity<SucursalRespuesta> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(new SucursalRespuesta(servicio.obtenerPorId(id)));
    }

    @PostMapping
    @RequiereRol(Roles.ADMIN_SISTEMA)
    @Operation(summary = "F3 · Crea una sucursal con su administrador inicial")
    public ResponseEntity<SucursalCreadaRespuesta> crear(@RequestBody @Valid NuevaSucursalSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(solicitud));
    }

    @PutMapping("/{id}")
    @RequiereRol(Roles.ADMIN_SISTEMA)
    @Operation(summary = "F4 · Edita una sucursal (el código no cambia)")
    public ResponseEntity<SucursalRespuesta> actualizar(@PathVariable UUID id,
                                                        @RequestBody @Valid ActualizarSucursalSolicitud solicitud) {
        return ResponseEntity.ok(servicio.actualizar(id, solicitud));
    }

    @PatchMapping("/{id}/estado")
    @RequiereRol(Roles.ADMIN_SISTEMA)
    @Operation(summary = "F5 · Activa o desactiva una sucursal")
    public ResponseEntity<SucursalRespuesta> cambiarEstado(@PathVariable UUID id,
                                                           @RequestBody @Valid CambiarEstadoSucursalSolicitud solicitud) {
        return ResponseEntity.ok(servicio.cambiarEstado(id, solicitud));
    }
}