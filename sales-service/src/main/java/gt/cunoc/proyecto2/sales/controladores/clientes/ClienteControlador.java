package gt.cunoc.proyecto2.sales.controladores.clientes;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.sales.dtos.clientes.ClienteRespuesta;
import gt.cunoc.proyecto2.sales.servicios.clientes.ClienteServicio;
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
@RequestMapping("/sales/clientes")
@Tag(name = "Clientes", description = "Clientes con cuenta, para vender a su nombre en la ventanilla")
public class ClienteControlador {

    private final ClienteServicio servicio;

    public ClienteControlador(ClienteServicio servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @RequiereRol({Roles.CAJERO, Roles.ADMIN_SUCURSAL, Roles.ADMIN_SISTEMA, Roles.CHOFER})
    @Operation(summary = "S4 · Busca clientes con cuenta por NIT, nombre o correo (máximo 20)")
    public ResponseEntity<List<ClienteRespuesta>> buscar(
            @Parameter(description = "NIT, nombre o correo. Vacío devuelve los primeros en orden alfabético.")
            @RequestParam(required = false) String busqueda) {
        return ResponseEntity.ok(servicio.buscar(busqueda));
    }
}
