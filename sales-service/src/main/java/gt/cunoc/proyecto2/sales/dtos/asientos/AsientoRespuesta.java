package gt.cunoc.proyecto2.sales.dtos.asientos;

import gt.cunoc.proyecto2.sales.modelos.asientos.AsientoViajeEntidad;
import java.time.OffsetDateTime;

public record AsientoRespuesta(int numero, EstadoMapaAsiento estado) {

    public AsientoRespuesta(AsientoViajeEntidad asiento, OffsetDateTime ahora) {
        this(asiento.getNumeroAsiento(),
                asiento.estaOcupado(ahora) ? EstadoMapaAsiento.OCUPADO : EstadoMapaAsiento.DISPONIBLE);
    }

    public boolean disponible() {
        return estado == EstadoMapaAsiento.DISPONIBLE;
    }
}