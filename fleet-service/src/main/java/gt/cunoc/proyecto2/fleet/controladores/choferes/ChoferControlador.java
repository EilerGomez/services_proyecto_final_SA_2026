package gt.cunoc.proyecto2.fleet.controladores.choferes;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.dtos.choferes.ActualizarChoferSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.choferes.ActualizarLicenciaSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.choferes.ChoferRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.choferes.DesactivarChoferSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.choferes.NuevoChoferSolicitud;
import gt.cunoc.proyecto2.fleet.modelos.choferes.EstadoChofer;
import gt.cunoc.proyecto2.fleet.servicios.choferes.ChoferServicio;
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
@RequestMapping("/fleet/choferes")
@Tag(name = "Choferes", description = "Datos laborales y licencia de los choferes")
public class ChoferControlador {

    private final ChoferServicio servicio;

    public ChoferControlador(ChoferServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @RequiereRol({Roles.ADMIN_SISTEMA, Roles.ADMIN_SUCURSAL})
    @Operation(summary = "F15 · Lista choferes (ADMIN_SUCURSAL: solo los de su sucursal)")
    public ResponseEntity<List<ChoferRespuesta>> listar(
            @RequestParam(required = false) UUID sucursalId,
            @RequestParam(required = false) EstadoChofer estado,
            @Parameter(hidden = true) UsuarioActual usuario) {
        List<ChoferRespuesta> choferes = servicio.listar(sucursalId, estado, usuario).stream()
                .map(ChoferRespuesta::new)
                .toList();
        return ResponseEntity.ok(choferes);
    }

    @GetMapping("/mio")
    @RequiereRol(Roles.CHOFER)
    @Operation(summary = "F17 · Datos laborales y licencia del chofer que inició sesión")
    public ResponseEntity<ChoferRespuesta> obtenerPropio(@Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(new ChoferRespuesta(servicio.obtenerPropio(usuario)));
    }

    @GetMapping("/{id}")
    @RequiereRol({Roles.ADMIN_SISTEMA, Roles.ADMIN_SUCURSAL})
    @Operation(summary = "F16 · Detalle de un chofer")
    public ResponseEntity<ChoferRespuesta> obtener(@PathVariable UUID id,
                                                   @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(new ChoferRespuesta(servicio.obtener(id, usuario)));
    }

    @PostMapping
    @RequiereRol(Roles.ADMIN_SUCURSAL)
    @Operation(summary = "F18 · Registra un chofer y crea su cuenta (la foto se sube antes con F22)")
    public ResponseEntity<ChoferRespuesta> crear(@RequestBody @Valid NuevoChoferSolicitud solicitud,
                                                 @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(solicitud, usuario));
    }

    @PutMapping("/{id}")
    @RequiereRol(Roles.ADMIN_SUCURSAL)
    @Operation(summary = "F19 · Edita el salario base por viaje")
    public ResponseEntity<ChoferRespuesta> actualizar(@PathVariable UUID id,
                                                      @RequestBody @Valid ActualizarChoferSolicitud solicitud,
                                                      @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.actualizar(id, solicitud, usuario));
    }

    @PutMapping("/{id}/licencia")
    @RequiereRol(Roles.ADMIN_SUCURSAL)
    @Operation(summary = "F20 · Renueva o corrige la licencia")
    public ResponseEntity<ChoferRespuesta> actualizarLicencia(@PathVariable UUID id,
                                                              @RequestBody @Valid ActualizarLicenciaSolicitud solicitud,
                                                              @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.actualizarLicencia(id, solicitud, usuario));
    }

    @PatchMapping("/{id}/desactivar")
    @RequiereRol(Roles.ADMIN_SUCURSAL)
    @Operation(summary = "F21 · Desactiva un chofer (consulta R3: no debe tener viajes activos)")
    public ResponseEntity<ChoferRespuesta> desactivar(@PathVariable UUID id,
                                                      @RequestBody @Valid DesactivarChoferSolicitud solicitud,
                                                      @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.desactivar(id, solicitud, usuario));
    }
}