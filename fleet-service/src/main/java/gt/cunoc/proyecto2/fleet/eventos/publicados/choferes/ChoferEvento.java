package gt.cunoc.proyecto2.fleet.eventos.publicados.choferes;

import gt.cunoc.proyecto2.fleet.modelos.choferes.ChoferEntidad;
import gt.cunoc.proyecto2.fleet.modelos.choferes.EstadoChofer;
import gt.cunoc.proyecto2.fleet.modelos.choferes.TipoLicencia;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Payload de ChoferCreado y ChoferActualizado (mismo contenido). Consumen: Trips, Reporting. */
public record ChoferEvento(
        UUID choferId,
        UUID usuarioId,
        UUID sucursalId,
        String nombreCompleto,
        String numeroLicencia,
        TipoLicencia tipoLicencia,
        LocalDate fechaVencimientoLicencia,
        BigDecimal salarioBaseViaje,
        EstadoChofer estado) {

    public static ChoferEvento desde(ChoferEntidad chofer) {
        return new ChoferEvento(chofer.getId(), chofer.getUsuarioId(), chofer.getSucursalId(),
                chofer.getNombreCompleto(), chofer.getNumeroLicencia(), chofer.getTipoLicencia(),
                chofer.getFechaVencimientoLicencia(), chofer.getSalarioBaseViaje(), chofer.getEstado());
    }
}