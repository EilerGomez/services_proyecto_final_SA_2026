package gt.cunoc.proyecto2.sales.dtos.historial;

import gt.cunoc.proyecto2.sales.modelos.alquileres.EstadoOrdenAlquiler;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.MetodoPago;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record AlquilerHistorialRespuesta(
        UUID ordenId,
        UUID alquilerId,
        String origen,
        String destino,
        BigDecimal monto,
        MetodoPago metodoPago,
        LocalDate fechaPago,
        EstadoOrdenAlquiler estado) {

    public AlquilerHistorialRespuesta(OrdenAlquilerEntidad orden) {
        this(orden.getId(), orden.getAlquilerId(), orden.getOrigen(), orden.getDestino(), orden.getMonto(),
                orden.getMetodoPago(), orden.getFechaPago(), orden.getEstado());
    }
}