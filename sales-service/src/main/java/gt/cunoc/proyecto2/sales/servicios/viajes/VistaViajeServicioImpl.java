package gt.cunoc.proyecto2.sales.servicios.viajes;

import gt.cunoc.proyecto2.sales.errores.viajes.ErrorViaje;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import gt.cunoc.proyecto2.sales.repositorios.viajes.VistaViajeRepositorio;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VistaViajeServicioImpl implements VistaViajeServicio {

    private final VistaViajeRepositorio repositorio;

    public VistaViajeServicioImpl(VistaViajeRepositorio repositorio) {
        this.repositorio = repositorio;
    }

    @Override
    @Transactional(readOnly = true)
    public VistaViajeEntidad obtenerALaVenta(UUID viajeId) {
        return repositorio.findById(viajeId)
                .filter(VistaViajeEntidad::estaALaVenta)// si esta PROGRAMADO
                .orElseThrow(() -> ErrorViaje.VIAJE_NO_DISPONIBLE.excepcion());
    }
}