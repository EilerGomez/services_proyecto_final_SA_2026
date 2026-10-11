package gt.cunoc.proyecto2.sales.eventos.publicados.ventas;

import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.CanalVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.MetodoPago;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import gt.cunoc.proyecto2.sales.utilidades.Fechas;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/*
 * Payload de BoletoVendido (venta CONFIRMADA: S4 o CarteraDebitada).
 * Consumen: Reporting (ingreso por sucursal, ruta y bus) y Notification (boletos al correo del cliente).
 */
public record BoletoVendido(
        UUID ventaId,
        UUID viajeId,
        UUID rutaId,
        UUID busId,
        String busPlaca,
        UUID sucursalId,
        CanalVenta canal,
        UUID clienteUsuarioId,
        String clienteNit,
        String clienteNombre,
        String clienteCorreo,
        List<Integer> asientos,
        int cantidadBoletos,
        BigDecimal montoTotal,
        MetodoPago metodoPago,
        LocalDate fechaPago,
        String origenNombre,
        String destinoNombre,
        OffsetDateTime fechaSalida) {

    public static BoletoVendido desde(VentaEntidad venta, List<BoletoEntidad> boletos, VistaViajeEntidad viaje) {
        List<Integer> asientos = boletos.stream().map(BoletoEntidad::getNumeroAsiento).sorted().toList();
        return new BoletoVendido(venta.getId(), venta.getViajeId(), viaje.getRutaId(), viaje.getBusId(),
                viaje.getBusPlaca(), venta.getSucursalId(), venta.getCanal(), venta.getClienteUsuarioId(),
                venta.getClienteNit(), venta.getClienteNombre(), venta.getClienteCorreo(), asientos,
                asientos.size(), venta.getMontoTotal(), venta.getMetodoPago(), venta.getFechaPago(),
                viaje.getOrigenNombre(), viaje.getDestinoNombre(), Fechas.enGuatemala(viaje.getFechaSalida()));
    }
}
