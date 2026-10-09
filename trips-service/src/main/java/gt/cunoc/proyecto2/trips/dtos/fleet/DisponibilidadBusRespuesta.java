package gt.cunoc.proyecto2.trips.dtos.fleet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DisponibilidadBusRespuesta(
        UUID busId,
        UUID sucursalId,
        String placa,
        int capacidadPasajeros,
        String estado,
        boolean disponible,
        String motivo) {
}