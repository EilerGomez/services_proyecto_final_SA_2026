package gt.cunoc.proyecto2.sales.controladores.historial;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.dtos.historial.HistorialRespuesta;
import gt.cunoc.proyecto2.sales.servicios.historial.HistorialServicio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sales/historial")
@Tag(name = "Historial", description = "Compras de boletos y alquileres por NIT")
public class HistorialControlador {

    private final HistorialServicio servicio;

    public HistorialControlador(HistorialServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping("/mio")
    @RequiereRol(Roles.CLIENTE)
    @Operation(summary = "S7 · Mi historial (por el NIT de mi perfil, incluye compras en mostrador)")
    public ResponseEntity<HistorialRespuesta> mio(@Parameter(hidden = true) UsuarioActual usuario) {
        return ResponseEntity.ok(servicio.delCliente(usuario));
    }

    @GetMapping
    @RequiereRol({Roles.CAJERO, Roles.ADMIN_SUCURSAL, Roles.ADMIN_SISTEMA})
    @Operation(summary = "S8 · Historial de un NIT")
    public ResponseEntity<HistorialRespuesta> porNit(@RequestParam String nit) {
        return ResponseEntity.ok(servicio.porNit(nit));
    }
}