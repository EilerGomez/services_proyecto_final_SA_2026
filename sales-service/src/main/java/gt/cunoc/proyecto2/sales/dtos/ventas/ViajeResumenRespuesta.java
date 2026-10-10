package gt.cunoc.proyecto2.sales.dtos.ventas;

import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import gt.cunoc.proyecto2.sales.utilidades.Fechas;
import java.time.OffsetDateTime;

public record ViajeResumenRespuesta(String origenNombre, String destinoNombre, OffsetDateTime fechaSalida,
                                    String busPlaca) {

    public ViajeResumenRespuesta(VistaViajeEntidad viaje) {
        this(viaje.getOrigenNombre(), viaje.getDestinoNombre(), Fechas.enGuatemala(viaje.getFechaSalida()),
                viaje.getBusPlaca());
    }
}