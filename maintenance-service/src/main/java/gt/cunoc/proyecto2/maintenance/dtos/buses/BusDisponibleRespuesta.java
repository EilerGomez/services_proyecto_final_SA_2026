package gt.cunoc.proyecto2.maintenance.dtos.buses;

import gt.cunoc.proyecto2.maintenance.modelos.VistaBusEntidad;
import java.util.UUID;

public record BusDisponibleRespuesta(
        UUID busId,
        String placa,
        UUID sucursalId,
        boolean activo) {

    public BusDisponibleRespuesta(VistaBusEntidad bus) {
        this(bus.getBusId(), bus.getPlaca(), bus.getSucursalId(), bus.isActivo());
    }
}
