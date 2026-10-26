package gt.cunoc.proyecto2.sales.dtos.clientes;

import gt.cunoc.proyecto2.sales.modelos.clientes.VistaClienteEntidad;
import java.util.UUID;

/** Datos de una cuenta que el personal puede asociar a una venta de mostrador. */
public record ClienteCuentaRespuesta(UUID usuarioId, String nit, String nombre, String correo) {

    public ClienteCuentaRespuesta(VistaClienteEntidad cliente) {
        this(cliente.getUsuarioId(), cliente.getNit(), cliente.getNombre(), cliente.getCorreo());
    }
}
