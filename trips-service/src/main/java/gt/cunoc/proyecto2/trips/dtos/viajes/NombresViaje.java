package gt.cunoc.proyecto2.trips.dtos.viajes;

import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.buses.VistaBusEntidad;
import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Datos de otras tablas para mostrar un viaje: rutas, alquileres, nombres de sucursales
 * (vista_sucursales), buses (vista_buses, con placa y foto) y nombres de choferes (vista_choferes).
 * Se cargan una sola vez por listado.
 */
public record NombresViaje(
        Map<UUID, RutaEntidad> rutas,
        Map<UUID, AlquilerPrivadoEntidad> alquileres,
        Map<UUID, String> sucursales,
        Map<UUID, VistaBusEntidad> buses,
        Map<UUID, String> choferes) {

    /** El alquiler del viaje, solo si es PRIVADO (un REGULAR va por ruta, no por alquiler). */
    private AlquilerPrivadoEntidad alquilerDe(ViajeEntidad viaje) {
        return viaje.esPrivado() ? alquileres.get(viaje.getAlquilerId()) : null;
    }

    /*nombre de la sucursal de origen de la ruta. PRIVADO: el origen que escribió el cliente. */
    public String origenNombre(ViajeEntidad viaje) {
        if (viaje.esPrivado()) {
            AlquilerPrivadoEntidad alquiler = alquilerDe(viaje);
            return alquiler == null ? null : alquiler.getOrigen();
        }
        RutaEntidad ruta = rutas.get(viaje.getRutaId());
        return ruta == null ? null : sucursales.get(ruta.getSucursalOrigenId());
    }

    /*nombre de la sucursal de destino de la ruta. PRIVADO: el destino que escribió el cliente. */
    public String destinoNombre(ViajeEntidad viaje) {
        if (viaje.esPrivado()) {
            AlquilerPrivadoEntidad alquiler = alquilerDe(viaje);
            return alquiler == null ? null : alquiler.getDestino();
        }
        RutaEntidad ruta = rutas.get(viaje.getRutaId());
        return ruta == null ? null : sucursales.get(ruta.getSucursalDestinoId());
    }

    /*
     * Puntos que el cliente marcó en el mapa al pedir el alquiler: con ellos el detalle del viaje
     * PRIVADO dibuja su recorrido, igual que el REGULAR lo dibuja con las sucursales de su ruta.
     * Null en un REGULAR y también en un alquiler viejo que se pidió sin marcar el mapa.
     */
    public BigDecimal origenLatitud(ViajeEntidad viaje) {
        AlquilerPrivadoEntidad alquiler = alquilerDe(viaje);
        return alquiler == null ? null : alquiler.getOrigenLatitud();
    }

    public BigDecimal origenLongitud(ViajeEntidad viaje) {
        AlquilerPrivadoEntidad alquiler = alquilerDe(viaje);
        return alquiler == null ? null : alquiler.getOrigenLongitud();
    }

    public BigDecimal destinoLatitud(ViajeEntidad viaje) {
        AlquilerPrivadoEntidad alquiler = alquilerDe(viaje);
        return alquiler == null ? null : alquiler.getDestinoLatitud();
    }

    public BigDecimal destinoLongitud(ViajeEntidad viaje) {
        AlquilerPrivadoEntidad alquiler = alquilerDe(viaje);
        return alquiler == null ? null : alquiler.getDestinoLongitud();
    }

    public String placa(ViajeEntidad viaje) {
        VistaBusEntidad bus = buses.get(viaje.getBusId());
        return bus == null ? null : bus.getPlaca();
    }

    /** Foto del bus del viaje: la ve el pasajero al comprar para reconocerlo en la terminal. */
    public String fotoBus(ViajeEntidad viaje) {
        VistaBusEntidad bus = buses.get(viaje.getBusId());
        return bus == null ? null : bus.getFotoUrl();
    }

    public String chofer(ViajeEntidad viaje) {
        return choferes.get(viaje.getChoferId());
    }
}