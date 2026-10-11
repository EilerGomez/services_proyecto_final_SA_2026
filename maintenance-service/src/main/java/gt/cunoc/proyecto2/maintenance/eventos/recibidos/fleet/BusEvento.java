package gt.cunoc.proyecto2.maintenance.eventos.recibidos.fleet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Payload de BusCreado y BusActualizado: solo los campos que usa Maintenance. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BusEvento(
        UUID busId,
        UUID sucursalId,
        String placa,
        String estado) {
}
