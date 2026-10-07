package gt.cunoc.proyecto2.fleet.dtos.buses;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import gt.cunoc.proyecto2.fleet.dtos.bitacora.ViajeKilometrajeRespuesta;
import java.util.List;


public record BusDetalleRespuesta(
        @JsonUnwrapped BusRespuesta bus,
        List<ViajeKilometrajeRespuesta> ultimosViajes) {
}