package gt.cunoc.proyecto2.trips.dtos.viajes;

import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import java.util.Map;
import java.util.UUID;

/**
 * Datos de otras tablas para mostrar un viaje: rutas, alquileres, nombres de sucursales
 * (vista_sucursales), placas (vista_buses) y nombres de choferes (vista_choferes).
 * Se cargan una sola vez por listado.
 */
public record NombresViaje(
        Map<UUID, RutaEntidad> rutas,
        Map<UUID, AlquilerPrivadoEntidad> alquileres,
        Map<UUID, String> sucursales,
        Map<UUID, String> placas,
        Map<UUID, String> choferes) {

    /*nombre de la sucursal de origen de la ruta. PRIVADO: el origen que escribió el cliente. */
    public String origenNombre(ViajeEntidad viaje) {
        if (viaje.esPrivado()) {
            AlquilerPrivadoEntidad alquiler = alquileres.get(viaje.getAlquilerId());
            return alquiler == null ? null : alquiler.getOrigen();
        }
        RutaEntidad ruta = rutas.get(viaje.getRutaId());
        return ruta == null ? null : sucursales.get(ruta.getSucursalOrigenId());
    }

    /*nombre de la sucursal de destino de la ruta. PRIVADO: el destino que escribió el cliente. */
    public String destinoNombre(ViajeEntidad viaje) {
        if (viaje.esPrivado()) {
            AlquilerPrivadoEntidad alquiler = alquileres.get(viaje.getAlquilerId());
            return alquiler == null ? null : alquiler.getDestino();
        }
        RutaEntidad ruta = rutas.get(viaje.getRutaId());
        return ruta == null ? null : sucursales.get(ruta.getSucursalDestinoId());
    }

    public String placa(ViajeEntidad viaje) {
        return placas.get(viaje.getBusId());
    }

    public String chofer(ViajeEntidad viaje) {
        return choferes.get(viaje.getChoferId());
    }
}