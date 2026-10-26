package gt.cunoc.proyecto2.sales.dtos.alquileres;

import gt.cunoc.proyecto2.sales.modelos.alquileres.EstadoOrdenAlquiler;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.MetodoPago;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OrdenAlquilerRespuesta(
        UUID ordenId,
        UUID alquilerId,
        UUID sucursalId,
        String clienteNit,
        String clienteNombre,
        String clienteCorreo,
        String origen,
        String destino,
        /** Puntos del mapa del recorrido; null si la solicitud no los marcó. */
        BigDecimal origenLatitud,
        BigDecimal origenLongitud,
        BigDecimal destinoLatitud,
        BigDecimal destinoLongitud,
        BigDecimal monto,
        MetodoPago metodoPago,
        LocalDate fechaPago,
        EstadoOrdenAlquiler estado,
        String motivoRechazo,
        Integer version) {

    public OrdenAlquilerRespuesta(OrdenAlquilerEntidad orden) {
        this(orden.getId(),
                orden.getAlquilerId(),
                orden.getSucursalId(),
                orden.getClienteNit(),
                orden.getClienteNombre(),
                orden.getClienteCorreo(),
                orden.getOrigen(),
                orden.getDestino(),
                orden.getOrigenLatitud(),
                orden.getOrigenLongitud(),
                orden.getDestinoLatitud(),
                orden.getDestinoLongitud(),
                orden.getMonto(),
                orden.getMetodoPago(),
                orden.getFechaPago(),
                orden.getEstado(),
                orden.getMotivoRechazo(),
                orden.getVersion());
    }
}