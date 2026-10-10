package gt.cunoc.proyecto2.trips.dtos.viajes;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import gt.cunoc.proyecto2.trips.dtos.operaciones.LlegadaRespuesta;
import gt.cunoc.proyecto2.trips.dtos.operaciones.SalidaRespuesta;

/**
 * Respuesta de T9, T14 y T15: los datos del viaje (al mismo nivel, gracias a @JsonUnwrapped) más
 * "salida" y "llegada" cuando ya se registraron (si no, no aparecen en el JSON).
 */
public record ViajeDetalleRespuesta(
        @JsonUnwrapped ViajeRespuesta viaje,
        @JsonInclude(JsonInclude.Include.NON_NULL) SalidaRespuesta salida,
        @JsonInclude(JsonInclude.Include.NON_NULL) LlegadaRespuesta llegada) {
}