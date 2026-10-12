package gt.cunoc.proyecto2.notification.evento.recibido;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDate;
import java.util.UUID;

/*
 * Licencia de un chofer registrada o renovada en Fleet: se programan los recordatorios 30, 15 y 3 días antes.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record LicenciaChoferActualizada(
        UUID choferId,
        UUID usuarioId,
        UUID sucursalId,
        LocalDate fechaVencimientoLicencia) {
}