package gt.cunoc.proyecto2.identity.dtos.usuario;

import java.time.Instant;
import java.util.UUID;

/**
 * Estado de la cuenta para otros servicios (Fleet lo consulta antes de reintegrar personal).
 * estado: ACTIVO, INACTIVO o BLOQUEADO; bloqueadoHasta solo viene en BLOQUEADO.
 */
public record EstadoCuentaResponse(

        UUID usuarioId,
        String estado,
        Instant bloqueadoHasta

) {
}
