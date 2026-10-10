package gt.cunoc.proyecto2.trips.servicios.sucursales;

import gt.cunoc.proyecto2.trips.errores.sucursales.ErrorSucursal;
import gt.cunoc.proyecto2.trips.modelos.sucursales.VistaSucursalEntidad;
import gt.cunoc.proyecto2.trips.repositorios.sucursales.VistaSucursalRepositorio;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VistaSucursalServicioImpl implements VistaSucursalServicio {

    private final VistaSucursalRepositorio repositorio;

    public VistaSucursalServicioImpl(VistaSucursalRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public VistaSucursalEntidad verificarActiva(UUID sucursalId) {
        VistaSucursalEntidad sucursal = repositorio.findById(sucursalId)
                .orElseThrow(() -> ErrorSucursal.SUCURSAL_NO_EXISTE.excepcion(sucursalId));
        if (!sucursal.isActiva()) {
            throw ErrorSucursal.SUCURSAL_INACTIVA.excepcion(sucursal.getNombre());
        }
        return sucursal;
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, String> nombres(Collection<UUID> sucursalIds) {
        return repositorio.findAllById(sucursalIds).stream()
                .collect(Collectors.toMap(VistaSucursalEntidad::getId, VistaSucursalEntidad::getNombre));
    }
}