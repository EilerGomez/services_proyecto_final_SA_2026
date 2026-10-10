package gt.cunoc.proyecto2.trips.controladores.viajes;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.dtos.viajes.ActualizarViajeSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.FiltroViajes;
import gt.cunoc.proyecto2.trips.dtos.viajes.NuevoViajeSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeDetalleRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeRespuesta;
import gt.cunoc.proyecto2.trips.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.TipoViaje;
import gt.cunoc.proyecto2.trips.servicios.viajes.ViajeServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
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
@RequestMapping("/trips/viajes")
@Tag(name = "Viajes", description = "Viajes regulares: búsqueda, programación, cambios y eliminación")
public class ViajeControlador {

    private final ViajeServicio servicio;

    public ViajeControlador(ViajeServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "T8 · Busca viajes (sin sesión: solo REGULAR PROGRAMADO; personal: los de su sucursal)")
    public ResponseEntity<List<ViajeRespuesta>> buscar(
            @RequestParam(required = false) UUID rutaId,
            @RequestParam(required = false) UUID sucursalId,
            @Parameter(description = "Día de salida desde (yyyy-MM-dd)") @RequestParam(required = false) LocalDate desde,
            @Parameter(description = "Día de salida hasta (yyyy-MM-dd)") @RequestParam(required = false) LocalDate hasta,
            @RequestParam(required = false) EstadoViaje estado,
            @RequestParam(required = false) TipoViaje tipo,
            @Parameter(hidden = true) @Nullable UsuarioActual usuario) {
        FiltroViajes filtro = new FiltroViajes(rutaId, sucursalId, null, estado, tipo, desde, hasta);
        return ResponseEntity.ok(servicio.buscar(filtro.paraUsuario(usuario)));
    }

    @GetMapping("/mios")
    @RequiereRol(Roles.CHOFER)
    @Operation(summary = "T13 · Viajes del chofer del token")
    public ResponseEntity<List<ViajeRespuesta>> mios(
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(required = false) EstadoViaje estado,
            @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.buscar(FiltroViajes.delChofer(usuario.choferId(), estado, desde, hasta)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "T9 · Detalle de un viaje con su salida y llegada (un PRIVADO solo para el personal de su sucursal y su chofer)")
    public ResponseEntity<ViajeDetalleRespuesta> obtener(@PathVariable UUID id,
                                                  @Parameter(hidden = true) @Nullable UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.obtener(id, usuario));
    }

    @PostMapping
    @RequiereRol(Roles.ADMIN_SUCURSAL)
    @Operation(summary = "T10 · Programa un viaje regular (valida bus y chofer con Flota)")
    public ResponseEntity<ViajeRespuesta> programar(@RequestBody @Valid NuevoViajeSolicitud solicitud,
                                                    @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.programar(solicitud, usuario));
    }

    @PutMapping("/{id}")
    @RequiereRol(Roles.ADMIN_SUCURSAL)
    @Operation(summary = "T11 · Cambia hora, bus o chofer de un viaje programado")
    public ResponseEntity<ViajeRespuesta> actualizar(@PathVariable UUID id,
                                                     @RequestBody @Valid ActualizarViajeSolicitud solicitud,
                                                     @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.actualizar(id, solicitud, usuario));
    }

    @DeleteMapping("/{id}")
    @RequiereRol(Roles.ADMIN_SUCURSAL)
    @Operation(summary = "T12 · Elimina un viaje programado (Ventas cancela y reembolsa los boletos)")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id,
                                         @Parameter(description = "Motivo que se avisa a los clientes")
                                         @RequestParam(required = false) String motivo,
                                         @Parameter(hidden = true) UsuarioActual usuario) {
        servicio.eliminar(id, motivo, usuario);
        return ResponseEntity.noContent().build();
    }
}