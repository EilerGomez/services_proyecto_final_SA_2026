package gt.cunoc.proyecto2.maintenance.dtos.servicios;

import gt.cunoc.proyecto2.maintenance.modelos.DetalleServicioEntidad;
import java.math.BigDecimal;
import java.util.UUID;

public record LineaServicioRespuesta(
        UUID lineaId,
        UUID categoriaId,
        String categoriaNombre,
        String descripcion,
        String tipo,
        BigDecimal monto) {

    public LineaServicioRespuesta(DetalleServicioEntidad linea) {
        this(linea.getId(), linea.getCategoriaId(),
                linea.getCategoria() != null ? linea.getCategoria().getNombre() : null,
                linea.getDescripcion(), linea.getTipo(), linea.getMonto());
    }
}
