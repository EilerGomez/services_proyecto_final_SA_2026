package gt.cunoc.proyecto2.maintenance.dtos.servicios;

import com.fasterxml.jackson.annotation.JsonInclude;
import gt.cunoc.proyecto2.maintenance.modelos.ServicioTallerEntidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ServicioRespuesta(
        UUID servicioId,
        UUID busId,
        String placa,
        UUID sucursalId,
        LocalDate fechaServicio,
        String taller,
        String observaciones,
        BigDecimal totalManoObra,
        BigDecimal totalRepuestos,
        BigDecimal total,
        UUID registradoPor,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<LineaServicioRespuesta> lineas) {

    public ServicioRespuesta(ServicioTallerEntidad servicio, String placa,
                             List<LineaServicioRespuesta> lineas) {
        this(servicio.getId(), servicio.getBusId(), placa, servicio.getSucursalId(),
                servicio.getFechaServicio(), servicio.getTaller(), servicio.getObservaciones(),
                servicio.getTotalManoObra(), servicio.getTotalRepuestos(), servicio.getTotal(),
                servicio.getRegistradoPor(), lineas);
    }
}
