package gt.cunoc.proyecto2.fleet.dtos.identity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Instant;
import java.util.UUID;

/**
 * Respuesta de R4c (GET /identity/interno/usuarios/{id}/estado).
 * estado: ACTIVO, INACTIVO o BLOQUEADO; bloqueadoHasta solo viene en BLOQUEADO.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EstadoCuentaRespuesta(
        UUID usuarioId,
        String estado,
        Instant bloqueadoHasta) {

    public boolean inactiva() {
        return !"ACTIVO".equals(estado) && !"BLOQUEADO".equals(estado);
    }

    /** Un bloqueo vencido no cuenta: Identity lo quita solo en el siguiente inicio de sesión. */
    public boolean bloqueada(Instant ahora) {
        return "BLOQUEADO".equals(estado) && (bloqueadoHasta == null || bloqueadoHasta.isAfter(ahora));
    }
}
