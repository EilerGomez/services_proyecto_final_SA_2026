package gt.cunoc.proyecto2.trips.servicios.sucursales;

import gt.cunoc.proyecto2.trips.modelos.sucursales.VistaSucursalEntidad;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

public interface VistaSucursalServicio {

    VistaSucursalEntidad verificarActiva(UUID sucursalId);

    Map<UUID, String> nombres(Collection<UUID> sucursalIds);
}