package gt.cunoc.proyecto2.sales.dtos.historial;

import java.util.List;

public record HistorialRespuesta(
        String nit,
        String nombre,
        List<VentaHistorialRespuesta> ventas,
        List<AlquilerHistorialRespuesta> alquileres) {
}