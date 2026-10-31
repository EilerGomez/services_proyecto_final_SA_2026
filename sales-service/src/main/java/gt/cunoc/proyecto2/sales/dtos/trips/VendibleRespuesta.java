package gt.cunoc.proyecto2.sales.dtos.trips;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/*
 * Respuesta de R2 · GET /trips/interno/viajes/{id}/vendible (siempre 200).
 * vendible = false con motivo (VIAJE_NO_EXISTE, VIAJE_PRIVADO, VIAJE_YA_INICIO) si no se puede vender.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record VendibleRespuesta(
        UUID viajeId,
        boolean vendible,
        String motivo,
        String estado,
        String tipo,
        OffsetDateTime fechaSalidaProgramada,
        BigDecimal precioBoleto,
        Integer capacidad,
        UUID sucursalOrigenId,
        /**
         * El piloto asignado puede vender boletos presenciales de este viaje: Trips ya comprobó que
         * la marca del viaje está activa y que el piloto sigue ACTIVO en Flota. Es Boolean y no
         * boolean a propósito: un Trips que todavía no mande el campo (un despliegue a medias) deja
         * la respuesta en null en vez de reventar la lectura del JSON, y eso vale como "no permitida".
         */
        Boolean ventaPorChofer,
        /** Piloto asignado al viaje, para comprobar que quien vende es él. */
        UUID choferId) {

    /** True solo si Trips dijo expresamente que el piloto puede vender boletos de este viaje. */
    public boolean permiteVentaPorChofer() {
        return Boolean.TRUE.equals(ventaPorChofer);
    }
}