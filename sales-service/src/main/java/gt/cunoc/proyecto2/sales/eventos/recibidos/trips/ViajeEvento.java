package gt.cunoc.proyecto2.sales.eventos.recibidos.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/*
 * Payload de ViajeProgramado y ViajeActualizado (de Trips), solo con los campos que usa Sales.
 * sucursalId es la sucursal de origen de la ruta: a ella se atribuye el ingreso.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViajeEvento(
        UUID viajeId,
        String tipo,
        UUID rutaId,
        UUID sucursalId,
        String origenNombre,
        String destinoNombre,
        UUID busId,
        String busPlaca,
        int capacidad,
        BigDecimal precioBoleto,
        OffsetDateTime fechaSalida) {

    /** Solo los viajes REGULAR se venden por boleto; los PRIVADO se pagan con su orden de alquiler. */
    public boolean esRegular() {
        return TiposViaje.REGULAR.equals(tipo);
    }
}
