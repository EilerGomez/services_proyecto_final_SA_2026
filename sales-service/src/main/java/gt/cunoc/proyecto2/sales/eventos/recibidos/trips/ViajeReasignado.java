package gt.cunoc.proyecto2.sales.eventos.recibidos.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.UUID;

/*
 * ViajeReasignado (Trips): a un viaje programado le cambiaron el bus, el piloto o los dos.
 *
 * Sales solo atiende los REGULAR: busca los boletos confirmados de ese viaje y publica un
 * BoletoReasignado por cada pasajero con correo. De un PRIVADO se encarga Notification, que recibe
 * el mismo evento con el correo del cliente del alquiler adentro.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViajeReasignado(
        UUID viajeId,
        String tipo,
        String origenNombre,
        String destinoNombre,
        OffsetDateTime fechaSalida,
        boolean cambioBus,
        String busPlacaAnterior,
        String busPlaca,
        boolean cambioChofer,
        String choferAnterior,
        String choferNombre) {

    public boolean esRegular() {
        return TiposViaje.REGULAR.equals(tipo);
    }
}
