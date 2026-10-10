package gt.cunoc.proyecto2.trips.controladores.alquileres;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.dtos.alquileres.AlquilerRespuesta;
import gt.cunoc.proyecto2.trips.dtos.alquileres.AsignarAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.CancelarAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.CotizarAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.FiltroAlquileres;
import gt.cunoc.proyecto2.trips.dtos.alquileres.NuevoAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeDetalleRespuesta;
import gt.cunoc.proyecto2.trips.modelos.alquileres.EstadoAlquiler;
import gt.cunoc.proyecto2.trips.servicios.alquileres.AlquilerServicio;
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
@RequestMapping("/trips/alquileres")
@Tag(name = "Alquileres privados", description = "Solicitud, cotización, cancelación y asignación de bus y chofer")
public class AlquilerControlador {

    private final AlquilerServicio servicio;

    public AlquilerControlador(AlquilerServicio servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @RequiereRol({Roles.CLIENTE, Roles.CAJERO})
    @Operation(summary = "T16 · Solicita un alquiler (CLIENTE en línea; CAJERO en mostrador)")
    public ResponseEntity<AlquilerRespuesta> solicitar(@RequestBody @Valid NuevoAlquilerSolicitud solicitud,
                                                       @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.solicitar(solicitud, usuario));
    }

    @GetMapping
    @RequiereRol({Roles.ADMIN_SISTEMA, Roles.ADMIN_SUCURSAL, Roles.CAJERO})
    @Operation(summary = "T17 · Lista los alquileres de la sucursal (ADMIN_SISTEMA: todos)")
    public ResponseEntity<List<AlquilerRespuesta>> listar(
            @RequestParam(required = false) UUID sucursalId,
            @RequestParam(required = false) EstadoAlquiler estado,
            @Parameter(description = "Día de salida desde (yyyy-MM-dd)") @RequestParam(required = false) LocalDate desde,
            @Parameter(description = "Día de salida hasta (yyyy-MM-dd)") @RequestParam(required = false) LocalDate hasta,
            @Parameter(hidden = true) UsuarioActual usuario) {
        FiltroAlquileres filtro = new FiltroAlquileres(sucursalId, estado, desde, hasta);
        return ResponseEntity.ok(servicio.listar(filtro.paraUsuario(usuario)));
    }

    @GetMapping("/mios")
    @RequiereRol(Roles.CLIENTE)
    @Operation(summary = "T18 · Alquileres del cliente del token")
    public ResponseEntity<List<AlquilerRespuesta>> mios(@Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.mios(usuario));
    }

    @GetMapping("/{id}")
    @Operation(summary = "T19 · Detalle (el cliente dueño o el personal de la sucursal)")
    public ResponseEntity<AlquilerRespuesta> obtener(@PathVariable UUID id,
                                                     @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.obtener(id, usuario));
    }

    @PostMapping("/{id}/cotizacion")
    @RequiereRol(Roles.ADMIN_SUCURSAL)
    @Operation(summary = "T20 · Confirma o ajusta el precio (Ventas crea la orden de pago)")
    public ResponseEntity<AlquilerRespuesta> cotizar(@PathVariable UUID id,
                                                     @RequestBody @Valid CotizarAlquilerSolicitud solicitud,
                                                     @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.cotizar(id, solicitud, usuario));
    }

    @PostMapping("/{id}/cancelar")
    @RequiereRol({Roles.CLIENTE, Roles.ADMIN_SUCURSAL})
    @Operation(summary = "T21 · Cancela un alquiler SOLICITADO")
    public ResponseEntity<AlquilerRespuesta> cancelar(@PathVariable UUID id,
                                                      @RequestBody @Valid CancelarAlquilerSolicitud solicitud,
                                                      @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.cancelar(id, solicitud, usuario));
    }

    @PostMapping("/{id}/asignacion")
    @RequiereRol(Roles.ADMIN_SUCURSAL)
    @Operation(summary = "T22 · Asigna bus y chofer a un alquiler PAGADO (crea su viaje PRIVADO)")
    public ResponseEntity<ViajeDetalleRespuesta> asignar(@PathVariable UUID id,
                                                         @RequestBody @Valid AsignarAlquilerSolicitud solicitud,
                                                         @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.asignar(id, solicitud, usuario));
    }
}