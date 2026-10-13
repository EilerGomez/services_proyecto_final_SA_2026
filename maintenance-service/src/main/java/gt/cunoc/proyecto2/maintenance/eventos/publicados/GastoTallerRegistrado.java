package gt.cunoc.proyecto2.maintenance.eventos.publicados;

import gt.cunoc.proyecto2.maintenance.modelos.DetalleServicioEntidad;
import gt.cunoc.proyecto2.maintenance.modelos.ServicioTallerEntidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Payload de GastoTallerRegistrado (catálogo de eventos). Consume: Reporting.
 * Los totales los calcula el trigger: se arma después de saveAndFlush + refresh.
 */
public record GastoTallerRegistrado(
        UUID servicioId,
        UUID busId,
        UUID sucursalId,
        LocalDate fechaServicio,
        String taller,
        BigDecimal totalManoObra,
        BigDecimal totalRepuestos,
        BigDecimal total,
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
                servicio.getSucursalId(), servicio.getFechaServicio(), servicio.getTaller(),
                servicio.getTotalManoObra(), servicio.getTotalRepuestos(), servicio.getTotal(),
                lineas);
    }

    private static Linea linea(DetalleServicioEntidad detalle) {
        return new Linea(detalle.getId(), detalle.getCategoriaId(),
                detalle.getCategoria() != null ? detalle.getCategoria().getNombre() : null,
                detalle.getDescripcion(), detalle.getTipo(), detalle.getMonto());
    }
}
