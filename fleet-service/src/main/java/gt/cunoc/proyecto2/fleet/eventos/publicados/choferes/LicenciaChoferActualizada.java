package gt.cunoc.proyecto2.fleet.eventos.publicados.choferes;

import gt.cunoc.proyecto2.fleet.modelos.choferes.ChoferEntidad;
import java.time.LocalDate;
import java.util.UUID;

/** Payload de LicenciaChoferActualizada. Consume: Notification (recordatorios 30/15/3 días antes). */
public record LicenciaChoferActualizada(
        UUID choferId,
        UUID usuarioId,
        UUID sucursalId,
        LocalDate fechaVencimientoLicencia) {

    public static LicenciaChoferActualizada desde(ChoferEntidad chofer) {
        return new LicenciaChoferActualizada(chofer.getId(), chofer.getUsuarioId(), chofer.getSucursalId(),
                chofer.getFechaVencimientoLicencia());
    }
}