package gt.cunoc.proyecto2.fleet.eventos.publicados.buses;

import gt.cunoc.proyecto2.fleet.modelos.buses.BusEntidad;
import gt.cunoc.proyecto2.fleet.modelos.buses.EstadoBus;
import java.math.BigDecimal;
import java.util.UUID;

/** Payload de BusCreado y BusActualizado (mismo contenido). Consumen: Trips, Maintenance, Reporting. */
public record BusEvento(
        UUID busId,
        UUID sucursalId,
        String placa,
        String marca,
        String modelo,
        int anioFabricacion,
        int capacidadPasajeros,
        EstadoBus estado,
        BigDecimal kilometrajeActual) {

    public static BusEvento desde(BusEntidad bus) {
        return new BusEvento(bus.getId(), bus.getSucursalId(), bus.getPlaca(), bus.getMarca(), bus.getModelo(),
                bus.getAnioFabricacion(), bus.getCapacidadPasajeros(), bus.getEstado(), bus.getKilometrajeActual());
    }
}