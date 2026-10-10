package gt.cunoc.proyecto2.trips.eventos.publicados.viajes;

import gt.cunoc.proyecto2.trips.modelos.operaciones.LlegadaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.operaciones.SalidaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.TipoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.utilidades.Fechas;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/*
 * Payload de ViajeCompletado. Consumen: Fleet (bitácora y kilometraje del bus) y Reporting (costos).
 * Los costos van ya calculados (los guardó llegadas_viaje), así Reporting no recalcula nada.
 */
public record ViajeCompletado(
        UUID viajeId,
        TipoViaje tipo,
        UUID rutaId,
        UUID alquilerId,
        UUID sucursalId,
        UUID busId,
        UUID choferId,
        OffsetDateTime fechaHoraLlegada,
        BigDecimal kmInicial,
        BigDecimal kmFinal,
        BigDecimal kmRecorridos,
        BigDecimal costoCombustible,
        BigDecimal tarifaDepreciacion,
        BigDecimal costoDepreciacion,
        BigDecimal salarioChofer) {

    public static ViajeCompletado desde(ViajeEntidad viaje, SalidaViajeEntidad salida, LlegadaViajeEntidad llegada) {
        return new ViajeCompletado(viaje.getId(), viaje.getTipo(), viaje.getRutaId(), viaje.getAlquilerId(),
                viaje.getSucursalId(), salida.getBusId(), salida.getChoferId(),
                Fechas.enGuatemala(llegada.getFechaHoraLlegada()), salida.getKmInicial(), llegada.getKmFinal(),
                llegada.getKmRecorridos(), llegada.getCostoCombustible(), llegada.getTarifaDepreciacion(),
                llegada.getCostoDepreciacion(), llegada.getSalarioChofer());
    }
}