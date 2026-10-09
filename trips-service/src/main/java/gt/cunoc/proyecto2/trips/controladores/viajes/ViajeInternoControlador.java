package gt.cunoc.proyecto2.trips.controladores.viajes;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.trips.dtos.viajes.VendibleRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajesActivosRespuesta;
import gt.cunoc.proyecto2.trips.servicios.viajes.ViajeServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints internos de viajes (solo otros servicios con token de servicio).
 * El gateway bloquea /trips/interno/**.
 */
@RestController
@RequestMapping("/trips/interno/viajes")
@RequiereRol(Roles.SERVICIO)
@Tag(name = "Viajes (interno)", description = "R2 para Ventas y R3 para Flota")
public class ViajeInternoControlador {

    private final ViajeServicio servicio;

    public ViajeInternoControlador(ViajeServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{id}/vendible")
    @Operation(summary = "R2 · ¿Se pueden vender boletos de este viaje? (siempre 200)")
    public ResponseEntity<VendibleRespuesta> vendible(@PathVariable UUID id) {
        return ResponseEntity.ok(servicio.consultarVendible(id));
    }

    @GetMapping("/activos")
    @Operation(summary = "R3 · Viajes PROGRAMADO o EN_RUTA de un bus (?busId=) o de un chofer (?choferId=)")
    public ResponseEntity<ViajesActivosRespuesta> activos(@RequestParam(required = false) UUID busId,
                                                          @RequestParam(required = false) UUID choferId) {
        return ResponseEntity.ok(servicio.viajesActivos(busId, choferId));
    }
}