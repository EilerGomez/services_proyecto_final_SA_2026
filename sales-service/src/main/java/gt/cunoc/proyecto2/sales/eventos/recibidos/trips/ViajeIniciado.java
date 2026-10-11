package gt.cunoc.proyecto2.sales.eventos.recibidos.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/* Payload de ViajeIniciado (de Trips): desde aquí el viaje ya no se vende. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViajeIniciado(
        UUID viajeId,
        String tipo) {

    public boolean esRegular() {
        return TiposViaje.REGULAR.equals(tipo);
    }
}
