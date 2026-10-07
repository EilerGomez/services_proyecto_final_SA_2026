package gt.cunoc.proyecto2.fleet.eventos.publicados.choferes;

import gt.cunoc.proyecto2.fleet.modelos.choferes.ChoferEntidad;
import java.util.UUID;

/** Payload de ChoferDesactivado. Consumen: Trips, Reporting, Identity (le quita el rol CHOFER). */
public record ChoferDesactivado(
        UUID choferId,
        UUID usuarioId,
        UUID sucursalId) {

    public static ChoferDesactivado desde(ChoferEntidad chofer) {
        return new ChoferDesactivado(chofer.getId(), chofer.getUsuarioId(), chofer.getSucursalId());
    }
}