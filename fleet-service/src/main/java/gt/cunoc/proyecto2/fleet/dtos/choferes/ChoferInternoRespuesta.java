package gt.cunoc.proyecto2.fleet.dtos.choferes;

import gt.cunoc.proyecto2.fleet.modelos.choferes.ChoferEntidad;
import gt.cunoc.proyecto2.fleet.modelos.choferes.EstadoChofer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Respuesta de R1b · GET /fleet/interno/choferes/{id}?fecha= (la usa Trips al programar un viaje).
 * licenciaVigente se calcula con la fecha del viaje; salarioBaseViaje es la copia que guarda Trips.
 */
public record ChoferInternoRespuesta(
        UUID choferId,
        UUID sucursalId,
        UUID usuarioId,
        String nombreCompleto,
        EstadoChofer estado,
        BigDecimal salarioBaseViaje,
        LocalDate fechaVencimientoLicencia,
        boolean licenciaVigente) {

    public ChoferInternoRespuesta(ChoferEntidad chofer, LocalDate fecha) {
        this(chofer.getId(),
                chofer.getSucursalId(),
                chofer.getUsuarioId(),
                chofer.getNombreCompleto(),
                chofer.getEstado(),
                chofer.getSalarioBaseViaje(),
                chofer.getFechaVencimientoLicencia(),
                chofer.licenciaVigenteEn(fecha));
    }
}