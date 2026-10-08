package gt.cunoc.proyecto2.fleet.eventos.publicados.buses;

import gt.cunoc.proyecto2.fleet.modelos.buses.BusEntidad;
import java.util.UUID;

/** Payload de BusDesactivado. Consumen: Trips, Maintenance, Reporting. */
public record BusDesactivado(
        UUID busId,
        UUID sucursalId,
        String placa) {

    public static BusDesactivado desde(BusEntidad bus) {
        return new BusDesactivado(bus.getId(), bus.getSucursalId(), bus.getPlaca());
    }
}