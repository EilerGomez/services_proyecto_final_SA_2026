package gt.cunoc.proyecto2.sales.controladores.tarifas;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.sales.dtos.tarifas.ActualizarTarifaReembolsoSolicitud;
import gt.cunoc.proyecto2.sales.dtos.tarifas.NuevaTarifaReembolsoSolicitud;
import gt.cunoc.proyecto2.sales.dtos.tarifas.ReglaReembolsoRespuesta;
import gt.cunoc.proyecto2.sales.dtos.tarifas.TarifaReembolsoRespuesta;
import gt.cunoc.proyecto2.sales.modelos.tarifas.EstadoAlquilerTarifa;
import gt.cunoc.proyecto2.sales.servicios.tarifas.TarifaReembolsoServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sales/tarifas-reembolso")
@Tag(name = "Tarifas de reembolso",
        description = "Porcentaje que se le devuelve al cliente cuando se cancela un alquiler")
public class TarifaReembolsoControlador {

    private final TarifaReembolsoServicio servicio;

    public TarifaReembolsoControlador(TarifaReembolsoServicio servicio) {
        this.servicio = servicio;
    }

    /**
     * La ve cualquier cuenta con sesión: el cliente la necesita para el aviso que aparece al pedir un
     * alquiler, y la ventanilla para explicarle la penalización antes de cancelar.
     */
    @GetMapping
    @Operation(summary = "Tabla de tarifas de reembolso (política de cancelación)")
    public ResponseEntity<List<TarifaReembolsoRespuesta>> listar() {
        return ResponseEntity.ok(servicio.listar());
    }

    @GetMapping("/aplicable")
    @Operation(summary = "Tarifa que aplicaría a una cancelación (para el aviso y el diálogo de cancelar)")
    public ResponseEntity<ReglaReembolsoRespuesta> aplicable(
            @Parameter(description = "Estado del alquiler al cancelar") @RequestParam EstadoAlquilerTarifa estado,
            @Parameter(description = "Horas que faltan para la salida") @RequestParam long horasAntes) {
        return ResponseEntity.ok(servicio.aplicable(estado, horasAntes));
    }

    @PostMapping
    @RequiereRol(Roles.ADMIN_SISTEMA)
    @Operation(summary = "Agrega un tramo a la tabla de tarifas de reembolso")
    public ResponseEntity<TarifaReembolsoRespuesta> crear(
            @RequestBody @Valid NuevaTarifaReembolsoSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(solicitud));
    }

    @PutMapping("/{id}")
    @RequiereRol(Roles.ADMIN_SISTEMA)
    @Operation(summary = "Edita el porcentaje, la justificación o si el tramo permite cancelar")
    public ResponseEntity<TarifaReembolsoRespuesta> actualizar(
            @PathVariable UUID id,
            @RequestBody @Valid ActualizarTarifaReembolsoSolicitud solicitud) {
        return ResponseEntity.ok(servicio.actualizar(id, solicitud));
    }
}
