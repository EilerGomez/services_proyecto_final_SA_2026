package gt.cunoc.proyecto2.maintenance.servicios.buses;

import gt.cunoc.proyecto2.maintenance.repositorios.VistaBusRepositorio;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Proyección local de buses (upsert por version, SQL en el repositorio). Un bus EN_TALLER
 * o EN_RUTA sigue activo: se le puede registrar servicio. Solo INACTIVO lo desactiva.
 * Si no cambia ninguna fila, el evento era viejo: se deja en el log y se termina sin error.
 */
@Service
public class ProyeccionBusesImpl implements ProyeccionBuses {

    private static final Logger log = LoggerFactory.getLogger(ProyeccionBusesImpl.class);

    private final VistaBusRepositorio buses;

    public ProyeccionBusesImpl(VistaBusRepositorio buses) {
        this.buses = buses;
    }

    @Override
    @Transactional
    public void guardar(UUID busId, UUID sucursalId, String placa, String estado, Long version) {
        int filas = buses.guardar(busId, placa, sucursalId, !"INACTIVO".equals(estado), versionDe(version));
        registrarSiIgnorado(filas, busId, version);
    }

    @Override
    @Transactional
    public void desactivar(UUID busId, Long version) {
        registrarSiIgnorado(buses.desactivar(busId, versionDe(version)), busId, version);
    }

    /** Fleet siempre manda aggregateVersion; si faltara, 0 (solo entra si la fila no existe o también es 0). */
    private static long versionDe(Long version) {
        return version == null ? 0L : version;
    }

    private static void registrarSiIgnorado(int filas, UUID busId, Long version) {
        if (filas == 0) {
            log.info("Evento ignorado: bus {} ya tiene una versión más nueva o no existe "
                    + "(version del evento: {})", busId, version);
        }
    }
}
