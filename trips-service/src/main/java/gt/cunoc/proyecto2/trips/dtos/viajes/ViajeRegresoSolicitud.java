package gt.cunoc.proyecto2.trips.dtos.viajes;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * El viaje de vuelta que se programa junto con la ida (bloque opcional de POST /trips/viajes).
 *
 * <p>No lleva ruta: es la misma del viaje de ida, recorrida al revés. Sí lleva su propio bus, su
 * propio piloto y su propio horario, porque nada obliga a que vuelva la misma unidad; lo normal es
 * que sí, y el frontend los propone ya llenos con los de la ida.</p>
 */
public record ViajeRegresoSolicitud(
        @NotNull UUID busId,
        @NotNull UUID choferId,
        @NotNull OffsetDateTime fechaSalidaProgramada,
        @NotNull OffsetDateTime fechaLlegadaEstimada,
        /** Si el piloto del regreso puede vender boletos presenciales. Null se toma como false. */
        Boolean ventaPorChofer) {

    public boolean ventaPorChoferActiva() {
        return Boolean.TRUE.equals(ventaPorChofer);
    }
}
