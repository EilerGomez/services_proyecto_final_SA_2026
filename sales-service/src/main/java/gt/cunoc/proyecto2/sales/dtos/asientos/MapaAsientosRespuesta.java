package gt.cunoc.proyecto2.sales.dtos.asientos;

import gt.cunoc.proyecto2.sales.modelos.asientos.AsientoViajeEntidad;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import gt.cunoc.proyecto2.sales.utilidades.Fechas;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record MapaAsientosRespuesta(
        UUID viajeId,
        String origenNombre,
        String destinoNombre,
        OffsetDateTime fechaSalida,
        String busPlaca,
        BigDecimal precioBoleto,
        int capacidad,
        long disponibles,
        List<AsientoRespuesta> asientos) {

    public static MapaAsientosRespuesta de(VistaViajeEntidad viaje, List<AsientoViajeEntidad> asientos,
                                           OffsetDateTime ahora) {
        List<AsientoRespuesta> mapa = asientos.stream()
                .map(asiento -> new AsientoRespuesta(asiento, ahora))
                .toList();
        return new MapaAsientosRespuesta(viaje.getViajeId(), viaje.getOrigenNombre(), viaje.getDestinoNombre(),
                Fechas.enGuatemala(viaje.getFechaSalida()), viaje.getBusPlaca(), viaje.getPrecioBoleto(),
                viaje.getCapacidad(), mapa.stream().filter(AsientoRespuesta::disponible).count(), mapa);
    }
}