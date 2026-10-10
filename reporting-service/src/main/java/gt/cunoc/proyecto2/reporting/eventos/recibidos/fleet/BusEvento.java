package gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

/** Payload de BusCreado y BusActualizado (mismo contenido). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record BusEvento(
        UUID busId,
        UUID sucursalId,
        String placa,
        String marca,
        String modelo,
        Integer capacidadPasajeros,
        String estado,
        BigDecimal kilometrajeActual) {
}
