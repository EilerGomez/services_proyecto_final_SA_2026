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
        UUID sucursalOrigenId) {
}