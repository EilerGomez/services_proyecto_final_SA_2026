package gt.cunoc.proyecto2.trips.dtos.viajes;

import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import java.util.Map;
import java.util.UUID;

/**
 * Datos de otras tablas para mostrar un viaje: rutas, nombres de sucursales (vista_sucursales),
 * placas (vista_buses) y nombres de choferes (vista_choferes). Se cargan una sola vez por listado.
 */
public record NombresViaje(
        Map<UUID, RutaEntidad> rutas,
        Map<UUID, String> sucursales,
        Map<UUID, String> placas,
        Map<UUID, String> choferes) {

    /** Viaje REGULAR: nombre de la sucursal de origen de la ruta (PRIVADO: paso de alquileres). */
    public String origenNombre(ViajeEntidad viaje) {
        RutaEntidad ruta = viaje.getRutaId() == null ? null : rutas.get(viaje.getRutaId());
        return ruta == null ? null : sucursales.get(ruta.getSucursalOrigenId());
    }

    public String destinoNombre(ViajeEntidad viaje) {
        RutaEntidad ruta = viaje.getRutaId() == null ? null : rutas.get(viaje.getRutaId());
        return ruta == null ? null : sucursales.get(ruta.getSucursalDestinoId());
    }

    public String placa(ViajeEntidad viaje) {
        return placas.get(viaje.getBusId());
    }

    public String chofer(ViajeEntidad viaje) {
        return choferes.get(viaje.getChoferId());
    }
}