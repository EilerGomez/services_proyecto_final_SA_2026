package gt.cunoc.proyecto2.trips.eventos.publicados.viajes;

import gt.cunoc.proyecto2.trips.modelos.viajes.TipoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import java.util.UUID;

/** Payload de ViajeEliminado. Consumen: Sales (cancela boletos y pide reembolsos) y Reporting. */
public record ViajeEliminado(
        UUID viajeId,
        TipoViaje tipo,
        UUID sucursalId,
        String motivo) {

    public static ViajeEliminado desde(ViajeEntidad viaje, String motivo) {
        return new ViajeEliminado(viaje.getId(), viaje.getTipo(), viaje.getSucursalId(), motivo);
    }
}