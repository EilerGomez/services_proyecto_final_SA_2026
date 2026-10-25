package gt.cunoc.proyecto2.notification.evento.recibido;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

/*
 * Ruta modificada publicada por Trips (precio, distancia o estado). Sirve para avisar a los
 * administradores de las sucursales que revisen el precio del boleto.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record RutaActualizada(
        UUID rutaId,
        UUID sucursalOrigenId,
        UUID sucursalDestinoId,
        String origenNombre,
        String destinoNombre,
        BigDecimal distanciaKm,
        BigDecimal precioBoleto,
        boolean activa) {
}
