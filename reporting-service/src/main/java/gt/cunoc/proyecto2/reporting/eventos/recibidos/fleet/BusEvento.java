package gt.cunoc.proyecto2.reporting.eventos.recibidos.fleet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Datos de un bus en los eventos de creación y actualización de Fleet.
 *
 * <p>Trae la información que usan los reportes de flota y el mapa; el kilometraje solo se
 * actualiza cuando el evento lo incluye.
 */
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
