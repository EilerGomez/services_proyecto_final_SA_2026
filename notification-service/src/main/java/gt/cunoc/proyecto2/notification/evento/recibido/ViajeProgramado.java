package gt.cunoc.proyecto2.notification.evento.recibido;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.OffsetDateTime;
import java.util.UUID;

/*
 * Viaje recién programado en Trips (regular o privado). Acá se usa para avisarle al piloto que se
 * le asignó: su correo no viaja en el evento, solo su cuenta (choferUsuarioId), que se resuelve
 * contra Identity. Si el evento llega sin esa cuenta, no hay a quién escribirle.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ViajeProgramado(
        UUID viajeId,
        String tipo,
        UUID sucursalId,
        String origenNombre,
        String destinoNombre,
        String busPlaca,
        UUID choferId,
        String choferNombre,
        UUID choferUsuarioId,
        OffsetDateTime fechaSalida,
        OffsetDateTime fechaLlegadaEstimada,
        String sentido) {

    /** El viaje de vuelta de una ruta: se lo decimos al piloto para que no lo confunda con la ida. */
    public boolean esRegreso() {
        return "REGRESO".equals(sentido);
    }

    public boolean esPrivado() {
        return "PRIVADO".equals(tipo);
    }
}
