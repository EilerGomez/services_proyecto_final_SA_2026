package gt.cunoc.proyecto2.sales.eventos.recibidos.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/* Payload de ViajeEliminado (de Trips): dispara la compensación de las ventas del viaje. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViajeEliminado(
        UUID viajeId,
        String tipo,
        UUID sucursalId,
        String motivo) {

    public boolean esRegular() {
        return TiposViaje.REGULAR.equals(tipo);
    }
}
