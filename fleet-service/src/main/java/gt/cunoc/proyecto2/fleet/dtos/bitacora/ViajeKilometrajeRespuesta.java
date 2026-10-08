package gt.cunoc.proyecto2.fleet.dtos.bitacora;

import gt.cunoc.proyecto2.fleet.modelos.bitacora.BitacoraKilometrajeEntidad;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/** Un viaje de la bitácora (F11 ultimosViajes y F23). */
public record ViajeKilometrajeRespuesta(
        UUID viajeId,
        BigDecimal kmInicial,
        BigDecimal kmFinal,
        BigDecimal kmRecorridos,
        OffsetDateTime fechaLlegada) {

    public ViajeKilometrajeRespuesta(BitacoraKilometrajeEntidad registro) {
        this(registro.getViajeId(),
                registro.getKmInicial(),
                registro.getKmFinal(),
                registro.kmRecorridos(),
                registro.getFechaLlegada());
    }
}