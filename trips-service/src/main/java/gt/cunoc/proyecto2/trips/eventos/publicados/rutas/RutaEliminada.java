package gt.cunoc.proyecto2.trips.eventos.publicados.rutas;

import java.util.UUID;

/** Payload de RutaEliminada. Consume: Reporting. */
public record RutaEliminada(UUID rutaId) {
}