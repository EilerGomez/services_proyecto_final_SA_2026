package gt.cunoc.proyecto2.sales.servicios.viajes;

import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import java.util.UUID;

public interface VistaViajeServicio {

    VistaViajeEntidad obtenerALaVenta(UUID viajeId);
}