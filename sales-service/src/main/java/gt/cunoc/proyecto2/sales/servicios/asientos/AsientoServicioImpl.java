package gt.cunoc.proyecto2.sales.servicios.asientos;

import gt.cunoc.proyecto2.sales.dtos.asientos.MapaAsientosRespuesta;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import gt.cunoc.proyecto2.sales.repositorios.asientos.AsientoViajeRepositorio;
import gt.cunoc.proyecto2.sales.servicios.viajes.VistaViajeServicio;
import gt.cunoc.proyecto2.sales.utilidades.Fechas;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AsientoServicioImpl implements AsientoServicio {

    private final VistaViajeServicio viajes;
    private final AsientoViajeRepositorio asientos;

    public AsientoServicioImpl(VistaViajeServicio viajes, AsientoViajeRepositorio asientos) {
        this.viajes = viajes;
        this.asientos = asientos;
    }

    @Override
    @Transactional(readOnly = true)
    public MapaAsientosRespuesta mapa(UUID viajeId) {
        VistaViajeEntidad viaje = viajes.obtenerALaVenta(viajeId);
        return MapaAsientosRespuesta.de(viaje, asientos.findByViajeIdOrderByNumeroAsientoAsc(viajeId), Fechas.ahora());
    }
}