package gt.cunoc.proyecto2.fleet.dtos.buses;

import gt.cunoc.proyecto2.fleet.modelos.buses.BusEntidad;
import gt.cunoc.proyecto2.fleet.modelos.buses.EstadoBus;
import java.util.UUID;

/**
 * Respuesta de R1a · GET /fleet/interno/buses/{id}/disponibilidad (la usa Trips al programar un viaje).
 * motivo: BUS_INACTIVO o BUS_EN_TALLER cuando disponible = false.
 */
public record DisponibilidadBusRespuesta(
        UUID busId,
        UUID sucursalId,
        String placa,
        int capacidadPasajeros,
        EstadoBus estado,
        boolean disponible,
        String motivo) {

    public DisponibilidadBusRespuesta(BusEntidad bus) {
        this(bus.getId(),
                bus.getSucursalId(),
                bus.getPlaca(),
                bus.getCapacidadPasajeros(),
                bus.getEstado(),
                bus.getEstado().disponibleParaViajes(),
                bus.getEstado().disponibleParaViajes() ? null : "BUS_" + bus.getEstado().name());
    }
}