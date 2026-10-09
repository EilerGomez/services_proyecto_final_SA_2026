package gt.cunoc.proyecto2.trips.dtos.alquileres;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.errores.viajes.ErrorViaje;
import gt.cunoc.proyecto2.trips.modelos.alquileres.EstadoAlquiler;
import java.time.LocalDate;
import java.util.UUID;

/** Filtros de T17. desde y hasta son días de salida en Guatemala (inclusive). Los nulos no filtran. */
public record FiltroAlquileres(UUID sucursalId, EstadoAlquiler estado, LocalDate desde, LocalDate hasta) {

    public FiltroAlquileres {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw ErrorViaje.RANGO_FECHAS_INVALIDO.excepcion(desde, hasta);
        }
    }

    /** ADMIN_SISTEMA: todos. ADMIN_SUCURSAL y CAJERO: los de su sucursal (otra → 403 SUCURSAL_AJENA). */
    public FiltroAlquileres paraUsuario(UsuarioActual usuario) {
        if (usuario.esAdminSistema()) {
            return this;
        }
        usuario.verificarAccesoSucursal(sucursalId != null ? sucursalId : usuario.sucursalId());
        return new FiltroAlquileres(usuario.sucursalId(), estado, desde, hasta);
    }
}