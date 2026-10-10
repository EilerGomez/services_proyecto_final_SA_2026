package gt.cunoc.proyecto2.trips.eventos.publicados.viajes;

import gt.cunoc.proyecto2.trips.modelos.operaciones.SalidaViajeEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.TipoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.utilidades.Fechas;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Payload de ViajeIniciado. Consumen: Sales (cierra la venta del viaje), Fleet (bus EN_RUTA) y Reporting.
 */
public record ViajeIniciado(
        UUID viajeId,
        TipoViaje tipo,
        UUID busId,
        UUID choferId,
        OffsetDateTime fechaHoraSalida,
        BigDecimal kmInicial) {

    public static ViajeIniciado desde(ViajeEntidad viaje, SalidaViajeEntidad salida) {
        return new ViajeIniciado(viaje.getId(), viaje.getTipo(), salida.getBusId(), salida.getChoferId(),
                Fechas.enGuatemala(salida.getFechaHoraSalida()), salida.getKmInicial());
    }
}