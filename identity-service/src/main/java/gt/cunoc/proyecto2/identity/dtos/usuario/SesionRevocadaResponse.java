package gt.cunoc.proyecto2.identity.dtos.usuario;

import java.time.Instant;
import java.util.UUID;

/** Revocación vigente (para que el API Gateway la cargue al arrancar). */
public record SesionRevocadaResponse(

        UUID usuarioId,
        Instant tokensValidosDesde

) {
}
