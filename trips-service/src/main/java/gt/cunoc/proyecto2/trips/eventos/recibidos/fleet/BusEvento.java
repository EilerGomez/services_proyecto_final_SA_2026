package gt.cunoc.proyecto2.trips.eventos.recibidos.fleet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Payload de BusCreado y BusActualizado (de Fleet), solo con los campos que usa Trips. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BusEvento(
        UUID busId,
        UUID sucursalId,
        String placa,
        int capacidadPasajeros,
        String estado,
        String fotoUrl) {
}