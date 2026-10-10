package gt.cunoc.proyecto2.trips.eventos.publicados.viajes;

import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeRespuesta;
import gt.cunoc.proyecto2.trips.modelos.viajes.TipoViaje;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Payload de ViajeProgramado y ViajeActualizado (mismo contenido).
 * Consumen: Sales (crea o ajusta los asientos; ignora los PRIVADO) y Reporting.
 * REGULAR: rutaId y precioBoleto, alquilerId null. PRIVADO: alquilerId, sin ruta ni precio de boleto.
 */
public record ViajeEvento(
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
        int capacidad,
        BigDecimal precioBoleto,
        OffsetDateTime fechaSalida,
        OffsetDateTime fechaLlegadaEstimada) {

    /** Desde la respuesta ya armada: trae origen, destino y placa (fechas ya en -06:00). */
    public static ViajeEvento desde(ViajeRespuesta viaje) {
        return new ViajeEvento(viaje.viajeId(), viaje.tipo(), viaje.rutaId(), viaje.alquilerId(),
                viaje.sucursalId(), viaje.origenNombre(), viaje.destinoNombre(), viaje.busId(), viaje.busPlaca(),
                viaje.choferId(), viaje.capacidad(), viaje.precioBoleto(), viaje.fechaSalidaProgramada(),
                viaje.fechaLlegadaEstimada());
    }
}