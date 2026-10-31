package gt.cunoc.proyecto2.trips.dtos.viajes;

import gt.cunoc.proyecto2.trips.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.MotivoNoVendible;
import gt.cunoc.proyecto2.trips.modelos.viajes.TipoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.utilidades.Fechas;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/*
 * Respuesta de GET /trips/interno/viajes/{id}/vendible (la usa Sales antes de confirmar una venta).
 * Siempre 200: vendible = false con motivo si no se puede vender (es una pregunta, no un error).
 */
public record VendibleRespuesta(
        UUID viajeId,
        boolean vendible,
        MotivoNoVendible motivo,
        EstadoViaje estado,
        TipoViaje tipo,
        OffsetDateTime fechaSalidaProgramada,
        BigDecimal precioBoleto,
        Integer capacidad,
        UUID sucursalOrigenId,
        /**
         * El piloto asignado puede vender boletos presenciales de este viaje: la marca del viaje
         * está activa y el piloto sigue ACTIVO en Flota. Lo demás (que el viaje no haya salido) ya
         * lo dice vendible.
         */
        boolean ventaPorChofer,
        /** Piloto asignado, para que Ventas compruebe que quien vende es él. */
        UUID choferId) {

    public VendibleRespuesta(ViajeEntidad viaje, boolean choferActivo) {
        this(viaje.getId(),
                viaje.motivoNoVendible() == null,
                viaje.motivoNoVendible(),
                viaje.getEstado(),
                viaje.getTipo(),
                Fechas.enGuatemala(viaje.getFechaSalidaProgramada()),
                viaje.getPrecioBoleto(),
                viaje.getCapacidadBus(),
                viaje.getSucursalId(),
                viaje.isVentaPorChofer() && choferActivo,
                viaje.getChoferId());
    }

    public static VendibleRespuesta noExiste(UUID viajeId) {
        return new VendibleRespuesta(viajeId, false, MotivoNoVendible.VIAJE_NO_EXISTE,
                null, null, null, null, null, null, false, null);
    }
}