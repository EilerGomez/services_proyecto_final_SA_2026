package gt.cunoc.proyecto2.trips.dtos.viajes;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.errores.viajes.ErrorViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.TipoViaje;
import java.time.LocalDate;
import java.util.UUID;

public record FiltroViajes(
        UUID rutaId,
        UUID sucursalId,
        UUID choferId,
        EstadoViaje estado,
        TipoViaje tipo,
        LocalDate desde,
        LocalDate hasta) {

    public FiltroViajes {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw ErrorViaje.RANGO_FECHAS_INVALIDO.excepcion(desde, hasta);
        }
    }

    public FiltroViajes paraUsuario(UsuarioActual usuario) {
        if (usuario == null || !usuario.tieneAlgunRol(Roles.ADMIN_SISTEMA, Roles.ADMIN_SUCURSAL, Roles.CAJERO)) {
            return new FiltroViajes(rutaId, sucursalId, null, EstadoViaje.PROGRAMADO, TipoViaje.REGULAR, desde, hasta);
        }
        if (usuario.esAdminSistema()) {
            return this;
        }
        usuario.verificarAccesoSucursal(sucursalId != null ? sucursalId : usuario.sucursalId());
        return new FiltroViajes(rutaId, usuario.sucursalId(), choferId, estado, tipo, desde, hasta);
    }

    /** T13: los viajes del chofer del token (sin choferId no se filtra a nadie: 403). */
    public static FiltroViajes delChofer(UUID choferId, EstadoViaje estado, LocalDate desde, LocalDate hasta) {
        if (choferId == null) {
            throw ErrorViaje.SIN_CHOFER_ASOCIADO.excepcion();
        }
        return new FiltroViajes(null, null, choferId, estado, null, desde, hasta);
    }
}