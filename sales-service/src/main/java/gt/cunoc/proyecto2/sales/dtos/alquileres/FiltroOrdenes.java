package gt.cunoc.proyecto2.sales.dtos.alquileres;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.modelos.alquileres.EstadoOrdenAlquiler;
import java.util.UUID;

public record FiltroOrdenes(UUID sucursalId, EstadoOrdenAlquiler estado) {

    public FiltroOrdenes paraUsuario(UsuarioActual usuario) {
        if (usuario.esAdminSistema()) {
            return this;
        }
        usuario.verificarAccesoSucursal(sucursalId != null ? sucursalId : usuario.sucursalId());
        return new FiltroOrdenes(usuario.sucursalId(), estado);
    }
}