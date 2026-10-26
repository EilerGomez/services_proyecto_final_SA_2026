package gt.cunoc.proyecto2.sales.dtos.clientes;

import gt.cunoc.proyecto2.sales.modelos.clientes.VistaClienteEntidad;
import java.util.UUID;

/**
 * Cliente con cuenta, como lo ve la ventanilla al buscar por NIT. Sale de vista_clientes, la copia
 * local que Sales mantiene con ClienteRegistrado y PerfilActualizado.
 */
public record ClienteRespuesta(UUID usuarioId, String nit, String nombre, String correo) {

    public ClienteRespuesta(VistaClienteEntidad cliente) {
        this(cliente.getUsuarioId(), cliente.getNit(), cliente.getNombre(), cliente.getCorreo());
    }
}
