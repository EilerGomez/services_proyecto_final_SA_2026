package gt.cunoc.proyecto2.fleet.controladores.buses;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.fleet.dtos.buses.DisponibilidadBusRespuesta;
import gt.cunoc.proyecto2.fleet.servicios.buses.BusServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints internos de buses (solo otros servicios con token de servicio).
 * El gateway bloquea /fleet/interno/**.
 */
@RestController
@RequestMapping("/fleet/interno/buses")
@RequiereRol(Roles.SERVICIO)
@Tag(name = "Interno · Buses", description = "Llamadas de otros servicios (R1a)")
public class BusInternoControlador {

    private final BusServicio servicio;

    public BusInternoControlador(BusServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{id}/disponibilidad")
    @Operation(summary = "R1a · ¿Se le puede asignar un viaje a este bus? (lo llama Trips)")
    public ResponseEntity<DisponibilidadBusRespuesta> disponibilidad(@PathVariable UUID id) {
        return ResponseEntity.ok(servicio.consultarDisponibilidad(id));
    }
}