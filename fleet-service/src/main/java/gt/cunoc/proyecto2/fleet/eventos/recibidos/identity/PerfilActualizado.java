package gt.cunoc.proyecto2.fleet.eventos.recibidos.identity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.UUID;

/** Payload de PerfilActualizado (de Identity), solo con los campos que usa Fleet. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PerfilActualizado(
        UUID usuarioId,
        String nombres,
        String apellidos,
        String correo,
        UUID choferId) {

    /** Mismo formato que devuelve R5 en nombreCompleto. */
    public String nombreCompleto() {
        return (nombres.strip() + " " + apellidos.strip()).strip();
    }
}