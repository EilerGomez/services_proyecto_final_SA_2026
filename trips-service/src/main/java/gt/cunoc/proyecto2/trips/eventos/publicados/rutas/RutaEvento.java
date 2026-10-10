package gt.cunoc.proyecto2.trips.eventos.publicados.rutas;

import gt.cunoc.proyecto2.trips.dtos.rutas.RutaRespuesta;
import java.math.BigDecimal;
import java.util.UUID;

/** Payload de RutaCreada y RutaActualizada (mismo contenido). Consume: Reporting. */
public record RutaEvento(
        UUID rutaId,
        UUID sucursalOrigenId,
        UUID sucursalDestinoId,
        String origenNombre,
        String destinoNombre,
        BigDecimal distanciaKm,
        BigDecimal precioBoleto,
        boolean activa) {

    /** Desde la respuesta ya armada: trae los nombres de las sucursales. */
    public static RutaEvento desde(RutaRespuesta ruta) {
        return new RutaEvento(ruta.rutaId(), ruta.sucursalOrigenId(), ruta.sucursalDestinoId(),
                ruta.origenNombre(), ruta.destinoNombre(), ruta.distanciaKm(), ruta.precioBoleto(), ruta.activa());
    }
}