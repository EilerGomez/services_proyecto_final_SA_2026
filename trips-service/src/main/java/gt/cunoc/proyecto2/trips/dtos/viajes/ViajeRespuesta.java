package gt.cunoc.proyecto2.trips.dtos.viajes;

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
        UUID busId,
        String busPlaca,
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
                viaje.getBusId(),
                nombres.placa(viaje),
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