package gt.cunoc.proyecto2.trips.controladores.operaciones;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.dtos.operaciones.RegistrarLlegadaSolicitud;
import gt.cunoc.proyecto2.trips.dtos.operaciones.RegistrarSalidaSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeDetalleRespuesta;
import gt.cunoc.proyecto2.trips.servicios.operaciones.OperacionViajeServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/trips/viajes/{id}")
@RequiereRol({Roles.CHOFER, Roles.ADMIN_SUCURSAL})
@Tag(name = "Salida y llegada", description = "Registro inmutable de la salida y la llegada de un viaje, con sus costos")
public class OperacionViajeControlador {

    private final OperacionViajeServicio servicio;

    public OperacionViajeControlador(OperacionViajeServicio servicio) {
        this.servicio = servicio;
    }

    @PostMapping("/salida")
    @Operation(summary = "T14 · Registra la salida (el viaje pasa a EN_RUTA)")
    public ResponseEntity<ViajeDetalleRespuesta> registrarSalida(
            @PathVariable UUID id,
            @RequestBody @Valid RegistrarSalidaSolicitud solicitud,
            @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.registrarSalida(id, solicitud, usuario));
    }

    @PostMapping("/llegada")
    @Operation(summary = "T15 · Registra la llegada y calcula costos (el viaje pasa a COMPLETADO)")
    public ResponseEntity<ViajeDetalleRespuesta> registrarLlegada(
            @PathVariable UUID id,
            @RequestBody @Valid RegistrarLlegadaSolicitud solicitud,
            @Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.registrarLlegada(id, solicitud, usuario));
    }
}