package gt.cunoc.proyecto2.sales.dtos.ventas;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.errores.ventas.ErrorVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.CanalVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.EstadoVenta;
import java.time.LocalDate;
import java.util.UUID;

public record FiltroVentas(
        UUID viajeId,
        UUID sucursalId,
        EstadoVenta estado,
        CanalVenta canal,
        LocalDate desde,
        LocalDate hasta) {

    public FiltroVentas {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw ErrorVenta.RANGO_FECHAS_INVALIDO.excepcion(desde, hasta);
        }
    }

    public FiltroVentas paraUsuario(UsuarioActual usuario) {
        if (usuario.esAdminSistema()) {
            return this;
        }
        usuario.verificarAccesoSucursal(sucursalId != null ? sucursalId : usuario.sucursalId());
        return new FiltroVentas(viajeId, usuario.sucursalId(), estado, canal, desde, hasta);
    }
}