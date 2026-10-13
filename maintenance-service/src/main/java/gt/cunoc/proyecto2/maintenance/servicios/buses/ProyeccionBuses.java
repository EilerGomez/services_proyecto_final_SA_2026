package gt.cunoc.proyecto2.maintenance.servicios.buses;

import java.util.UUID;

public interface ProyeccionBuses {

    void guardar(UUID busId, UUID sucursalId, String placa, String estado, Long version);

    void desactivar(UUID busId, Long version);
}
