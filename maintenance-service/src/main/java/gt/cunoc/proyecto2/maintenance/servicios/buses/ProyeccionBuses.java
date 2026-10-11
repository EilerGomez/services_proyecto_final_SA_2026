package gt.cunoc.proyecto2.maintenance.servicios.buses;

import gt.cunoc.proyecto2.maintenance.modelos.VistaBusEntidad;
import gt.cunoc.proyecto2.maintenance.repositorios.VistaBusRepositorio;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proyección local de buses. Un bus EN_TALLER o EN_RUTA sigue activo:
 * se le puede registrar servicio. Solo INACTIVO lo desactiva.
 */
@Service
public class ProyeccionBuses {

    private final VistaBusRepositorio buses;

    public ProyeccionBuses(VistaBusRepositorio buses) {
        this.buses = buses;
    }

    @Transactional
    public void guardar(UUID busId, UUID sucursalId, String placa, String estado, Long version) {
        VistaBusEntidad bus = buses.findById(busId).orElseGet(VistaBusEntidad::new);
        if (bus.getBusId() == null) {
            bus.setBusId(busId);
        } else if (!versionVigente(bus.getVersion(), version)) {
            return;
        }
        bus.setSucursalId(sucursalId);
        bus.setPlaca(placa);
        bus.setActivo(!"INACTIVO".equals(estado));
        if (version != null) {
            bus.setVersion(version.intValue());
        }
        buses.save(bus);
    }

    @Transactional
    public void desactivar(UUID busId, Long version) {
        buses.findById(busId).ifPresent(bus -> {
            if (!versionVigente(bus.getVersion(), version)) {
                return;
            }
            bus.setActivo(false);
            if (version != null) {
                bus.setVersion(version.intValue());
            }
            buses.save(bus);
        });
    }

    private boolean versionVigente(Integer guardada, Long recibida) {
        if (recibida == null) {
            return true;
        }
        int actual = guardada != null ? guardada : 0;
        return recibida >= actual;
    }
}
