package gt.cunoc.proyecto2.fleet.dtos.choferes;

import gt.cunoc.proyecto2.fleet.modelos.choferes.ChoferEntidad;
import gt.cunoc.proyecto2.fleet.modelos.choferes.EstadoChofer;
import gt.cunoc.proyecto2.fleet.modelos.choferes.TipoLicencia;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Respuesta de F15–F21. La foto y el contacto del chofer están en Identity:
 * el frontend los pide con GET /identity/usuarios/{usuarioId}.
 */
public record ChoferRespuesta(
        UUID choferId,
        UUID sucursalId,
        UUID usuarioId,
        String nombreCompleto,
        String numeroLicencia,
        TipoLicencia tipoLicencia,
        LocalDate fechaVencimientoLicencia,
        BigDecimal salarioBaseViaje,
        EstadoChofer estado,
        Integer version) {

    public ChoferRespuesta(ChoferEntidad chofer) {
        this(chofer.getId(),
                chofer.getSucursalId(),
                chofer.getUsuarioId(),
                chofer.getNombreCompleto(),
                chofer.getNumeroLicencia(),
                chofer.getTipoLicencia(),
                chofer.getFechaVencimientoLicencia(),
                chofer.getSalarioBaseViaje(),
                chofer.getEstado(),
                chofer.getVersion());
    }
}