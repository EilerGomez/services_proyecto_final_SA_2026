package gt.cunoc.proyecto2.fleet.dtos.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/** Respuesta de R3 · GET /trips/interno/viajes/activos?busId= (o ?choferId=). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViajesActivosRespuesta(
        int cantidad,
        List<ViajeActivo> viajes) {

    public ViajesActivosRespuesta {
        viajes = viajes == null ? List.of() : List.copyOf(viajes);
    }

    public boolean hayViajes() {
        return cantidad > 0;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ViajeActivo(UUID viajeId, String estado, OffsetDateTime fechaSalidaProgramada) {
    }
}