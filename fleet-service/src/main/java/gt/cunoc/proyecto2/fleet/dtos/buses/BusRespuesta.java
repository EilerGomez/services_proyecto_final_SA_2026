package gt.cunoc.proyecto2.fleet.dtos.buses;

import gt.cunoc.proyecto2.fleet.modelos.buses.BusEntidad;
import gt.cunoc.proyecto2.fleet.modelos.buses.EstadoBus;
import java.math.BigDecimal;
import java.util.UUID;

/** Respuesta de F10–F14. */
public record BusRespuesta(
        UUID busId,
        UUID sucursalId,
        String placa,
        String marca,
        String modelo,
        int anioFabricacion,
        int capacidadPasajeros,
        EstadoBus estado,
        BigDecimal kilometrajeActual,
        String fotoUrl,
        Integer version) {

    public BusRespuesta(BusEntidad bus) {
        this(bus.getId(),
                bus.getSucursalId(),
                bus.getPlaca(),
                bus.getMarca(),
                bus.getModelo(),
                bus.getAnioFabricacion(),
                bus.getCapacidadPasajeros(),
                bus.getEstado(),
                bus.getKilometrajeActual(),
                bus.getFotoUrl(),
                bus.getVersion());
    }
}