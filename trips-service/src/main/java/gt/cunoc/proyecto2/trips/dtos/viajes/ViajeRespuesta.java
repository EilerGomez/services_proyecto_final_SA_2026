package gt.cunoc.proyecto2.trips.dtos.viajes;

import com.fasterxml.jackson.annotation.JsonInclude;
import gt.cunoc.proyecto2.trips.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.TipoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.utilidades.Fechas;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Respuesta de T8–T13. Las fechas salen con la hora de Guatemala (-06:00). */
public record ViajeRespuesta(
        UUID viajeId,
        TipoViaje tipo,
        UUID rutaId,
        UUID alquilerId,
        UUID sucursalId,
        String origenNombre,
        String destinoNombre,
        /*
         * Solo en un viaje PRIVADO: los puntos que el cliente marcó al pedir el alquiler, para
         * dibujar su recorrido en el detalle. En un REGULAR no van (la ruta trae sus sucursales).
         */
        @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal origenLatitud,
        @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal origenLongitud,
        @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal destinoLatitud,
        @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal destinoLongitud,
        UUID busId,
        String busPlaca,
        /** Foto del bus (de vista_buses): el pasajero la ve al comprar, sin necesitar sesión. */
        String busFotoUrl,
        UUID choferId,
        String choferNombre,
        int capacidad,
        BigDecimal precioBoleto,
        OffsetDateTime fechaSalidaProgramada,
        OffsetDateTime fechaLlegadaEstimada,
        EstadoViaje estado,
        Integer version) {

    public ViajeRespuesta(ViajeEntidad viaje, NombresViaje nombres) {
        this(viaje.getId(),
                viaje.getTipo(),
                viaje.getRutaId(),
                viaje.getAlquilerId(),
                viaje.getSucursalId(),
                nombres.origenNombre(viaje),
                nombres.destinoNombre(viaje),
                nombres.origenLatitud(viaje),
                nombres.origenLongitud(viaje),
                nombres.destinoLatitud(viaje),
                nombres.destinoLongitud(viaje),
                viaje.getBusId(),
                nombres.placa(viaje),
                nombres.fotoBus(viaje),
                viaje.getChoferId(),
                nombres.chofer(viaje),
                viaje.getCapacidadBus(),
                viaje.getPrecioBoleto(),
                Fechas.enGuatemala(viaje.getFechaSalidaProgramada()),
                Fechas.enGuatemala(viaje.getFechaLlegadaEstimada()),
                viaje.getEstado(),
                viaje.getVersion());
    }
}