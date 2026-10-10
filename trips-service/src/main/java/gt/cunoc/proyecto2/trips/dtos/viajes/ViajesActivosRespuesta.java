package gt.cunoc.proyecto2.trips.dtos.viajes;

import gt.cunoc.proyecto2.trips.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.utilidades.Fechas;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/*
 * Respuesta de GET /trips/interno/viajes/activos?busId= (o ?choferId=): viajes PROGRAMADO o
 * EN_RUTA. La usa Fleet antes de desactivar un bus o un chofer.
 */
public record ViajesActivosRespuesta(int cantidad, List<ViajeActivo> viajes) {

    public record ViajeActivo(UUID viajeId, EstadoViaje estado, OffsetDateTime fechaSalidaProgramada) {

        public ViajeActivo(ViajeEntidad viaje) {
            this(viaje.getId(), viaje.getEstado(), Fechas.enGuatemala(viaje.getFechaSalidaProgramada()));
        }
    }

    public ViajesActivosRespuesta(List<ViajeEntidad> viajes) {
        this(viajes.size(), viajes.stream().map(ViajeActivo::new).toList());
    }
}