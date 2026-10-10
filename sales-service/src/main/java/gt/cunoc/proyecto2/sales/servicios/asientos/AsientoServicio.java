package gt.cunoc.proyecto2.sales.servicios.asientos;

import gt.cunoc.proyecto2.sales.dtos.asientos.MapaAsientosRespuesta;
import java.util.UUID;

public interface AsientoServicio {

    MapaAsientosRespuesta mapa(UUID viajeId);
}