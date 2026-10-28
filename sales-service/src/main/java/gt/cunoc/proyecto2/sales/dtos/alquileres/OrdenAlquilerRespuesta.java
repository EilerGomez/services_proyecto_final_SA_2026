package gt.cunoc.proyecto2.sales.dtos.alquileres;

import gt.cunoc.proyecto2.sales.modelos.alquileres.EstadoOrdenAlquiler;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.MetodoPago;
import gt.cunoc.proyecto2.sales.utilidades.Fechas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
        /** Intervalo del alquiler (V10): la pantalla de reembolsos necesita saber de qué viaje era. */
        OffsetDateTime fechaSalida,
        OffsetDateTime fechaRetorno,
        BigDecimal monto,
        MetodoPago metodoPago,
        LocalDate fechaPago,
        EstadoOrdenAlquiler estado,
        String motivoRechazo,
        // ----- Cancelación y devolución (V14) -----
        LocalDate fechaCancelacion,
        String motivoCancelacion,
        Integer horasAntesViaje,
        BigDecimal porcentajeReembolso,
        String justificacionReembolso,
        BigDecimal montoReembolsado,
        LocalDate fechaReembolso,
        UUID reembolsadoPor,
        /** true mientras quede dinero por entregarle al cliente: es el filtro de la pantalla. */
        boolean reembolsoPendiente,
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
                Fechas.enGuatemala(orden.getFechaSalida()),
                Fechas.enGuatemala(orden.getFechaRetorno()),
                orden.getMonto(),
                orden.getMetodoPago(),
                orden.getFechaPago(),
                orden.getEstado(),
                orden.getMotivoRechazo(),
                orden.getFechaCancelacion(),
                orden.getMotivoCancelacion(),
                orden.getHorasAntesViaje(),
                orden.getPorcentajeReembolso(),
                orden.getJustificacionReembolso(),
                orden.getMontoReembolsado(),
                orden.getFechaReembolso(),
                orden.getReembolsadoPor(),
                orden.tieneReembolsoPendiente(),
                orden.getVersion());
    }
}
