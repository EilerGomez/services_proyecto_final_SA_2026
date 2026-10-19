package gt.cunoc.proyecto2.identity.dtos.usuario;

import java.time.Instant;
import java.util.UUID;

/**
 * Estado de la cuenta para otros servicios (Fleet lo consulta antes de reintegrar personal; Wallet antes
 * de debitar). estado: ACTIVO, INACTIVO o BLOQUEADO; bloqueadoHasta solo viene en BLOQUEADO.
 * impedimentoPersonal: motivo por el que la cuenta no puede ser personal de sucursal (es administrador
 * del sistema o chofer); null si puede.
 */
public record EstadoCuentaResponse(

        UUID usuarioId,
        String estado,
        Instant bloqueadoHasta,
        String impedimentoPersonal

) {

    public EstadoCuentaResponse(UUID usuarioId, String estado, Instant bloqueadoHasta) {
        this(usuarioId, estado, bloqueadoHasta, null);
    }
}
