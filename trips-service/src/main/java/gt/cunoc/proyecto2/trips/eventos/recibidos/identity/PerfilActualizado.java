package gt.cunoc.proyecto2.trips.eventos.recibidos.identity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/* Payload de PerfilActualizado (de Identity). El nombre viene partido en nombres y apellidos. */
@JsonIgnoreProperties(ignoreUnknown = true)
public record PerfilActualizado(
        UUID usuarioId,
        String nit,
        String nombres,
        String apellidos,
        String correo) {

    public String nombreCompleto() {
        return Stream.of(nombres, apellidos)
                .filter(Objects::nonNull)
                .map(String::strip)
                .filter(parte -> !parte.isEmpty())
                .collect(Collectors.joining(" "));
    }
}
