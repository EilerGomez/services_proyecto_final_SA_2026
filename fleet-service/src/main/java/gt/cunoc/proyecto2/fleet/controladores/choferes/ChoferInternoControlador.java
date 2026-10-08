package gt.cunoc.proyecto2.fleet.controladores.choferes;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.fleet.dtos.choferes.ChoferInternoRespuesta;
import gt.cunoc.proyecto2.fleet.servicios.choferes.ChoferServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints internos de choferes (solo otros servicios con token de servicio).
 * El gateway bloquea /fleet/interno/**.
 */
@RestController
@RequestMapping("/fleet/interno/choferes")
@RequiereRol(Roles.SERVICIO)
@Tag(name = "Interno · Choferes", description = "Llamadas de otros servicios (R1b)")
public class ChoferInternoControlador {

    private final ChoferServicio servicio;

    public ChoferInternoControlador(ChoferServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/{id}")
    @Operation(summary = "R1b · Estado, salario y vigencia de la licencia en la fecha del viaje (lo llama Trips)")
    public ResponseEntity<ChoferInternoRespuesta> consultar(
            @PathVariable UUID id,
            @Parameter(description = "Fecha del viaje (yyyy-MM-dd)") @RequestParam LocalDate fecha) {
        return ResponseEntity.ok(servicio.consultarParaViaje(id, fecha));
    }
}