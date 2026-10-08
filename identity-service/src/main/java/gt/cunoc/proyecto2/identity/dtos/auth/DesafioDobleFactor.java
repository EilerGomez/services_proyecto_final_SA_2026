package gt.cunoc.proyecto2.identity.dtos.auth;


import java.time.Instant;
import java.util.UUID;


/**
 * La cuenta exige segundo factor: se envió un código al correo.
 */
public record DesafioDobleFactor(
        UUID usuarioId,
        String correo,
        Instant expiraEn) implements ResultadoInicioSesion {
}
