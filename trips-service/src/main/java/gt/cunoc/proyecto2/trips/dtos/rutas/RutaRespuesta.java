package gt.cunoc.proyecto2.trips.dtos.rutas;

import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record RutaRespuesta(
        UUID rutaId,
        UUID sucursalOrigenId,
        String origenNombre,
        UUID sucursalDestinoId,
        String destinoNombre,
        BigDecimal distanciaKm,
        BigDecimal precioBoleto,
        boolean activa,
        Integer version) {

    /** nombres: sucursalId → nombre (de VistaSucursalServicio.nombres). */
    public RutaRespuesta(RutaEntidad ruta, Map<UUID, String> nombres) {
        this(ruta.getId(),
                ruta.getSucursalOrigenId(),
                nombres.get(ruta.getSucursalOrigenId()),
                ruta.getSucursalDestinoId(),
                nombres.get(ruta.getSucursalDestinoId()),
                ruta.getDistanciaKm(),
                ruta.getPrecioBoleto(),
                ruta.isActiva(),
                ruta.getVersion());
    }
}