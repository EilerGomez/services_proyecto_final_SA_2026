package gt.cunoc.proyecto2.trips.controladores.rutas;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.dtos.rutas.ActualizarRutaSolicitud;
import gt.cunoc.proyecto2.trips.dtos.rutas.NuevaRutaSolicitud;
import gt.cunoc.proyecto2.trips.dtos.rutas.RutaRespuesta;
import gt.cunoc.proyecto2.trips.servicios.rutas.RutaServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trips/rutas")
@Tag(name = "Rutas", description = "Rutas entre sucursales con su distancia y precio del boleto")
public class RutaControlador {

    private final RutaServicio servicio;

    public RutaControlador(RutaServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "T3 · Lista las rutas (público)")
    public ResponseEntity<List<RutaRespuesta>> listar(
            @RequestParam(required = false) UUID sucursalOrigenId,
            @Parameter(description = "true = activas, false = inactivas, vacío = todas")
            @RequestParam(required = false) Boolean activa) {
        return ResponseEntity.ok(servicio.listar(sucursalOrigenId, activa));
    }

    @GetMapping("/{id}")
    @Operation(summary = "T4 · Detalle de una ruta (público)")
    public ResponseEntity<RutaRespuesta> obtener(@PathVariable UUID id) {
        return ResponseEntity.ok(servicio.obtener(id));
    }

    @PostMapping
    @RequiereRol(Roles.ADMIN_SUCURSAL)
    @Operation(summary = "T5 · Crea una ruta desde la sucursal del administrador")
    public ResponseEntity<RutaRespuesta> crear(@RequestBody @Valid NuevaRutaSolicitud solicitud,
                                               @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(solicitud, usuario));
    }

    @PutMapping("/{id}")
    @RequiereRol(Roles.ADMIN_SUCURSAL)
    @Operation(summary = "T6 · Edita distancia, precio o si está activa")
    public ResponseEntity<RutaRespuesta> actualizar(@PathVariable UUID id,
                                                    @RequestBody @Valid ActualizarRutaSolicitud solicitud,
                                                    @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.actualizar(id, solicitud, usuario));
    }

    @DeleteMapping("/{id}")
    @RequiereRol(Roles.ADMIN_SUCURSAL)
    @Operation(summary = "T7 · Elimina una ruta sin viajes")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id, @Parameter(hidden = true) UsuarioActual usuario) {
        servicio.eliminar(id, usuario);
        return ResponseEntity.noContent().build();
    }
}