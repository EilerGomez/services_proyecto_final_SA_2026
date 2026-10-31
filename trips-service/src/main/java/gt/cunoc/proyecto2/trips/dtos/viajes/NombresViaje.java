package gt.cunoc.proyecto2.trips.dtos.viajes;

import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.buses.VistaBusEntidad;
import gt.cunoc.proyecto2.trips.modelos.choferes.VistaChoferEntidad;
import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
        Map<UUID, VistaChoferEntidad> choferes) {

    /** El alquiler del viaje, solo si es PRIVADO (un REGULAR va por ruta, no por alquiler). */
    private AlquilerPrivadoEntidad alquilerDe(ViajeEntidad viaje) {
        return viaje.esPrivado() ? alquileres.get(viaje.getAlquilerId()) : null;
    }

    /**
     * Dónde empieza el viaje. PRIVADO: el origen que escribió el cliente. REGULAR: la sucursal de
     * origen de su ruta, o la de destino si el viaje es de REGRESO, porque la vuelta recorre la
     * misma ruta al revés.
     *
     * <p>El cambio se hace aquí a propósito: así el recorrido sale ya en el orden correcto en la
     * respuesta, en los eventos y en todo lo que viva de ellos (Sales, Reporting, los correos), sin
     * que ninguno de ellos tenga que saber que existe la columna sentido.</p>
     */
    public String origenNombre(ViajeEntidad viaje) {
        if (viaje.esPrivado()) {
            AlquilerPrivadoEntidad alquiler = alquilerDe(viaje);
            return alquiler == null ? null : alquiler.getOrigen();
        }
        return puntaDeLaRuta(viaje, viaje.esRegreso());
    }

    /** Dónde termina el viaje: la otra punta de la ruta, con el mismo criterio que el origen. */
    public String destinoNombre(ViajeEntidad viaje) {
        if (viaje.esPrivado()) {
            AlquilerPrivadoEntidad alquiler = alquilerDe(viaje);
            return alquiler == null ? null : alquiler.getDestino();
        }
        return puntaDeLaRuta(viaje, !viaje.esRegreso());
    }

    /** Una de las dos puntas de la ruta del viaje: la de destino si {@code destino}, si no la de origen. */
    private String puntaDeLaRuta(ViajeEntidad viaje, boolean destino) {
        RutaEntidad ruta = rutas.get(viaje.getRutaId());
        if (ruta == null) {
            return null;
        }
        return sucursales.get(destino ? ruta.getSucursalDestinoId() : ruta.getSucursalOrigenId());
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

    /**
     * Fecha y hora de retorno que pidió el cliente del alquiler, o null si lo pidió solo de ida.
     * El piloto la necesita: la llegada estimada del viaje es la del trayecto, no la del regreso.
     */
    public OffsetDateTime fechaRetorno(ViajeEntidad viaje) {
        AlquilerPrivadoEntidad alquiler = alquilerDe(viaje);
        return alquiler == null ? null : alquiler.getFechaRetorno();
    }

    /** Correo del cliente del alquiler (puede no tener): a él se le avisa un cambio de bus o piloto. */
    public String clienteCorreo(ViajeEntidad viaje) {
        AlquilerPrivadoEntidad alquiler = alquilerDe(viaje);
        return alquiler == null ? null : alquiler.getClienteCorreo();
    }

    public String clienteNombre(ViajeEntidad viaje) {
        AlquilerPrivadoEntidad alquiler = alquilerDe(viaje);
        return alquiler == null ? null : alquiler.getClienteNombre();
    }

    public String placa(ViajeEntidad viaje) {
        VistaBusEntidad bus = buses.get(viaje.getBusId());
        return bus == null ? null : bus.getPlaca();
    }

    /**
     * Odómetro del bus según Fleet. Va en la respuesta para que quien registre la salida vea de
     * cuánto parte y no escriba un kilometraje menor, que el backend rechaza.
     */
    public BigDecimal kilometrajeBus(ViajeEntidad viaje) {
        VistaBusEntidad bus = buses.get(viaje.getBusId());
        return bus == null ? null : bus.getKilometrajeActual();
    }

    /** Foto del bus del viaje: la ve el pasajero al comprar para reconocerlo en la terminal. */
    public String fotoBus(ViajeEntidad viaje) {
        VistaBusEntidad bus = buses.get(viaje.getBusId());
        return bus == null ? null : bus.getFotoUrl();
    }

    public String chofer(ViajeEntidad viaje) {
        VistaChoferEntidad chofer = choferes.get(viaje.getChoferId());
        return chofer == null ? null : chofer.getNombreCompleto();
    }

    /**
     * La cuenta del piloto en Identity. Viaja en el evento para que Notification le pueda escribir
     * sin que Trips guarde correos; null si su copia todavía no trae la cuenta (eventos viejos).
     */
    public UUID choferUsuarioId(ViajeEntidad viaje) {
        VistaChoferEntidad chofer = choferes.get(viaje.getChoferId());
        return chofer == null ? null : chofer.getUsuarioId();
    }
}