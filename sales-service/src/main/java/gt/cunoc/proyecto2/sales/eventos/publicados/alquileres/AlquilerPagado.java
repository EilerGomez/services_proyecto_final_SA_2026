package gt.cunoc.proyecto2.sales.eventos.publicados.alquileres;

import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.MetodoPago;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/*
 * Payload de AlquilerPagado (S13 o CarteraDebitada de un ALQUILER).
 * Consumen: Trips (el alquiler pasa a PAGADO), Reporting (ingreso; lee monto y montoTotal, son el mismo valor)
 * y Notification (avisa al cliente).
 */
public record AlquilerPagado(
        UUID ordenId,
        UUID alquilerId,
        UUID sucursalId,
        UUID clienteUsuarioId,
        String clienteNit,
        String clienteNombre,
        String clienteCorreo,
        String origen,
        String destino,
        OffsetDateTime fechaSalida,
        OffsetDateTime fechaRetorno,
        BigDecimal monto,
        BigDecimal montoTotal,
        MetodoPago metodoPago,
        LocalDate fechaPago) {

    public static AlquilerPagado desde(OrdenAlquilerEntidad orden) {
        return new AlquilerPagado(orden.getId(), orden.getAlquilerId(), orden.getSucursalId(),
                orden.getClienteUsuarioId(), orden.getClienteNit(), orden.getClienteNombre(),
                orden.getClienteCorreo(), orden.getOrigen(), orden.getDestino(), orden.getFechaSalida(),
                orden.getFechaRetorno(), orden.getMonto(), orden.getMonto(),
                orden.getMetodoPago(), orden.getFechaPago());
    }
}
