package gt.cunoc.proyecto2.maintenance.controladores.buses;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.maintenance.dtos.buses.BusDisponibleRespuesta;
import gt.cunoc.proyecto2.maintenance.repositorios.VistaBusRepositorio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/maintenance/buses")
@Tag(name = "Buses", description = "Buses disponibles para registrar taller (vista local, sin llamar a Fleet)")
public class BusControlador {

    private final VistaBusRepositorio buses;

    public BusControlador(VistaBusRepositorio buses) {
        this.buses = buses;
    }

    @GetMapping
    @RequiereRol(Roles.ADMIN_SUCURSAL)
    @Operation(summary = "M4 · Buses de la sucursal del token para el selector del formulario")
    public ResponseEntity<List<BusDisponibleRespuesta>> listar(
            @Parameter(description = "true = activos, false = inactivos, vacío = todos")
            @RequestParam(required = false) Boolean activo,
            @Parameter(hidden = true) UsuarioActual usuario) {
        List<BusDisponibleRespuesta> busesSucursal = buses
                .findBySucursalIdOrderByPlacaAsc(usuario.sucursalId()).stream()
                .filter(b -> activo == null || b.isActivo() == activo)
                .map(BusDisponibleRespuesta::new)
                .toList();
        return ResponseEntity.ok(busesSucursal);
    }
}
