package gt.cunoc.proyecto2.sales.controladores.clientes;

import gt.cunoc.proyecto2.comun.seguridad.users.RequiereRol;
import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.sales.dtos.clientes.ClienteCuentaRespuesta;
import gt.cunoc.proyecto2.sales.modelos.clientes.VistaClienteEntidad;
import gt.cunoc.proyecto2.sales.repositorios.clientes.VistaClienteRepositorio;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sales/clientes")
@Tag(name = "Clientes", description = "Búsqueda de clientes con cuenta para ventas en mostrador")
public class ClientesControlador {

    private final VistaClienteRepositorio clientes;

    public ClientesControlador(VistaClienteRepositorio clientes) {
        this.clientes = clientes;
    }

    @GetMapping
    @RequiereRol({Roles.CAJERO, Roles.ADMIN_SUCURSAL, Roles.ADMIN_SISTEMA})
    @Operation(summary = "Busca hasta 20 clientes por NIT, nombre o correo")
    public ResponseEntity<List<ClienteCuentaRespuesta>> buscar(
            @RequestParam(required = false) String busqueda) {
        String texto = busqueda == null ? "" : busqueda.trim();
        List<VistaClienteEntidad> encontrados = texto.isEmpty()
                ? clientes.findTop20ByOrderByNombreAsc()
                : clientes.findTop20ByNitContainingIgnoreCaseOrNombreContainingIgnoreCaseOrCorreoContainingIgnoreCaseOrderByNombreAsc(
                        texto, texto, texto);
        return ResponseEntity.ok(encontrados.stream().map(ClienteCuentaRespuesta::new).toList());
    }
}
