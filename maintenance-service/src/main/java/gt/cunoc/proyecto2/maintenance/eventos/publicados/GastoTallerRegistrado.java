package gt.cunoc.proyecto2.maintenance.eventos.publicados;

import gt.cunoc.proyecto2.maintenance.modelos.DetalleServicioEntidad;
import gt.cunoc.proyecto2.maintenance.modelos.ServicioTallerEntidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Payload de GastoTallerRegistrado. Consume: Reporting. */
public record GastoTallerRegistrado(
        UUID servicioId,
        UUID busId,
        UUID sucursalId,
        LocalDate fechaServicio,
        List<Linea> lineas) {

    public record Linea(
            UUID lineaId,
            UUID categoriaId,
            String categoriaNombre,
            String descripcion,
            String tipo,
            BigDecimal monto) {
    }

    public static GastoTallerRegistrado desde(ServicioTallerEntidad servicio) {
        List<Linea> lineas = servicio.getLineas().stream()
                .map(GastoTallerRegistrado::linea)
                .toList();
        return new GastoTallerRegistrado(servicio.getId(), servicio.getBusId(),
                servicio.getSucursalId(), servicio.getFechaServicio(), lineas);
    }

    private static Linea linea(DetalleServicioEntidad detalle) {
        return new Linea(detalle.getId(), detalle.getCategoriaId(),
                detalle.getCategoria() != null ? detalle.getCategoria().getNombre() : null,
                detalle.getDescripcion(), detalle.getTipo(), detalle.getMonto());
    }
}
