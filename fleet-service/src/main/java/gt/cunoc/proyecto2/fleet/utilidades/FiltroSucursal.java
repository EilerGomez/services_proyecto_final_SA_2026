package gt.cunoc.proyecto2.fleet.utilidades;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import java.util.UUID;

/**
 * Sucursal con la que se filtra un listado (F10 buses, F15 choferes).
 * ADMIN_SISTEMA: la que pida, o todas si no manda ninguna.
 * ADMIN_SUCURSAL: siempre la de su token; si pide otra → 403 SUCURSAL_AJENA.
 */
public final class FiltroSucursal {

    private FiltroSucursal() {
    }

    public static UUID resolver(UsuarioActual usuario, UUID sucursalSolicitada) {
        if (usuario.esAdminSistema()) {
            return sucursalSolicitada;
        }
        usuario.verificarAccesoSucursal(sucursalSolicitada != null ? sucursalSolicitada : usuario.sucursalId());
        return usuario.sucursalId();
    }
}