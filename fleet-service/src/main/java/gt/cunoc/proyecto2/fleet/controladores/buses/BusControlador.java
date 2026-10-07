package gt.cunoc.proyecto2.fleet.controladores.buses;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.dtos.buses.ActualizarBusSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.buses.BusRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.buses.CambiarEstadoBusSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.buses.NuevoBusSolicitud;
import gt.cunoc.proyecto2.fleet.modelos.buses.EstadoBus;
import gt.cunoc.proyecto2.fleet.servicios.buses.BusServicio;
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
@RequestMapping("/fleet/buses")
@Tag(name = "Buses", description = "Flota de buses de cada sucursal")
public class BusControlador {

    private final BusServicio servicio;

    public BusControlador(BusServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @RequiereRol({Roles.ADMIN_SISTEMA, Roles.ADMIN_SUCURSAL})
    @Operation(summary = "F10 · Lista buses (ADMIN_SUCURSAL: solo los de su sucursal)")
    public ResponseEntity<List<BusRespuesta>> listar(
            @RequestParam(required = false) UUID sucursalId,
            @RequestParam(required = false) EstadoBus estado,
            @Parameter(hidden = true) UsuarioActual usuario) {
        List<BusRespuesta> buses = servicio.listar(sucursalId, estado, usuario).stream()
                .map(BusRespuesta::new)
                .toList();
        return ResponseEntity.ok(buses);
    }

    @GetMapping("/{id}")
    @RequiereRol({Roles.ADMIN_SISTEMA, Roles.ADMIN_SUCURSAL})
    @Operation(summary = "F11 · Detalle de un bus")
    public ResponseEntity<BusRespuesta> obtener(@PathVariable UUID id,
                                                @Parameter(hidden = true) UsuarioActual usuario) {
        // Paso bitacora_kilometraje: agregar los últimos viajes del bus a la respuesta.
        return ResponseEntity.ok(new BusRespuesta(servicio.obtener(id, usuario)));
    }

    @PostMapping
    @RequiereRol(Roles.ADMIN_SUCURSAL)
    @Operation(summary = "F12 · Registra un bus (la foto se sube antes con F22)")
    public ResponseEntity<BusRespuesta> crear(@RequestBody @Valid NuevoBusSolicitud solicitud,
                                              @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(solicitud, usuario));
    }

    @PutMapping("/{id}")
    @RequiereRol(Roles.ADMIN_SUCURSAL)
    @Operation(summary = "F13 · Edita marca, modelo, año, capacidad y foto")
    public ResponseEntity<BusRespuesta> actualizar(@PathVariable UUID id,
                                                   @RequestBody @Valid ActualizarBusSolicitud solicitud,
                                                   @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.actualizar(id, solicitud, usuario));
    }

    @PatchMapping("/{id}/estado")
    @RequiereRol(Roles.ADMIN_SUCURSAL)
    @Operation(summary = "F14 · Cambia el estado: ACTIVO, EN_TALLER o INACTIVO (INACTIVO consulta R3)")
    public ResponseEntity<BusRespuesta> cambiarEstado(@PathVariable UUID id,
                                                      @RequestBody @Valid CambiarEstadoBusSolicitud solicitud,
                                                      @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.cambiarEstado(id, solicitud, usuario));
    }
}