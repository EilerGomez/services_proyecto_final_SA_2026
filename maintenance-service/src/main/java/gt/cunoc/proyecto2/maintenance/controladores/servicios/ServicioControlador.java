package gt.cunoc.proyecto2.maintenance.controladores.servicios;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.maintenance.dtos.servicios.NuevoServicioSolicitud;
import gt.cunoc.proyecto2.maintenance.dtos.servicios.ServicioRespuesta;
import gt.cunoc.proyecto2.maintenance.servicios.taller.ServicioTallerServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/maintenance/servicios")
@Tag(name = "Servicios de taller", description = "Registro y consulta de servicios (no se editan ni se eliminan)")
public class ServicioControlador {

    private final ServicioTallerServicio servicio;

    public ServicioControlador(ServicioTallerServicio servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @RequiereRol(Roles.ADMIN_SUCURSAL)
    @Operation(summary = "M5 · Registra un servicio de taller con su detalle (publica GastoTallerRegistrado)")
    public ResponseEntity<ServicioRespuesta> registrar(
            @RequestBody @Valid NuevoServicioSolicitud solicitud,
            @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.registrar(solicitud, usuario));
    }

    @GetMapping
    @RequiereRol({Roles.ADMIN_SISTEMA, Roles.ADMIN_SUCURSAL})
    @Operation(summary = "M6 · Lista servicios (sin líneas)")
    public ResponseEntity<List<ServicioRespuesta>> listar(
            @RequestParam(required = false) UUID busId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(required = false) UUID categoriaId,
            @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.listar(busId, desde, hasta, categoriaId, usuario));
    }

    @GetMapping("/{id}")
    @RequiereRol({Roles.ADMIN_SISTEMA, Roles.ADMIN_SUCURSAL})
    @Operation(summary = "M7 · Detalle de un servicio con sus líneas")
    public ResponseEntity<ServicioRespuesta> obtener(
            @PathVariable UUID id,
            @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.obtener(id, usuario));
    }
}
