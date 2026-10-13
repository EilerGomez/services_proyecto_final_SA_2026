package gt.cunoc.proyecto2.maintenance.controladores.categorias;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.maintenance.dtos.categorias.ActualizarCategoriaSolicitud;
import gt.cunoc.proyecto2.maintenance.dtos.categorias.CategoriaRespuesta;
import gt.cunoc.proyecto2.maintenance.dtos.categorias.NuevaCategoriaSolicitud;
import gt.cunoc.proyecto2.maintenance.servicios.categorias.CategoriaServicio;
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
@RequestMapping("/maintenance/categorias")
@Tag(name = "Categorías", description = "Catálogo de categorías de gasto de taller")
public class CategoriaControlador {

    private final CategoriaServicio servicio;

    public CategoriaControlador(CategoriaServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @RequiereRol({Roles.ADMIN_SISTEMA, Roles.ADMIN_SUCURSAL})
    @Operation(summary = "M1 · Lista las categorías")
    public ResponseEntity<List<CategoriaRespuesta>> listar(
            @Parameter(description = "true = activas, false = inactivas, vacío = todas")
            @RequestParam(required = false) Boolean activa) {
        return ResponseEntity.ok(servicio.listar(activa));
    }

    @PostMapping
    @RequiereRol(Roles.ADMIN_SISTEMA)
    @Operation(summary = "M2 · Crea una categoría (publica CategoriaGastoActualizada)")
    public ResponseEntity<CategoriaRespuesta> crear(
            @RequestBody @Valid NuevaCategoriaSolicitud solicitud) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicio.crear(solicitud));
    }

    @PutMapping("/{id}")
    @RequiereRol(Roles.ADMIN_SISTEMA)
    @Operation(summary = "M3 · Renombra, activa o desactiva una categoría (publica CategoriaGastoActualizada)")
    public ResponseEntity<CategoriaRespuesta> actualizar(
            @PathVariable UUID id,
            @RequestBody @Valid ActualizarCategoriaSolicitud solicitud) {
        return ResponseEntity.ok(servicio.actualizar(id, solicitud));
    }
}
