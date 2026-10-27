package gt.cunoc.proyecto2.sales.eventos.publicados.ventas;

import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.ViajeReasignado;
import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/*
 * Payload de BoletoReasignado: al viaje de este boleto le cambiaron el bus, el piloto o los dos.
 * Consume: Notification (le escribe al pasajero para que no busque la unidad equivocada).
 *
 * Se publica uno por cada venta confirmada con correo, no uno por viaje con la lista adentro: así
 * cada correo falla o se reintenta por su cuenta y la idempotencia por eventoId sigue valiendo.
 */
public record BoletoReasignado(
        UUID ventaId,
        String codigo,
        UUID viajeId,
        String clienteNombre,
        String clienteCorreo,
        List<Integer> asientos,
        String origenNombre,
        String destinoNombre,
        OffsetDateTime fechaSalida,
        boolean cambioBus,
        String busPlacaAnterior,
        String busPlaca,
        boolean cambioChofer,
        String choferAnterior,
        String choferNombre) {

    public static BoletoReasignado desde(VentaEntidad venta, List<BoletoEntidad> boletos, ViajeReasignado viaje) {
        List<Integer> asientos = boletos.stream().map(BoletoEntidad::getNumeroAsiento).sorted().toList();
        return new BoletoReasignado(venta.getId(), venta.codigo(), venta.getViajeId(),
                venta.getClienteNombre(), venta.getClienteCorreo(), asientos,
                viaje.origenNombre(), viaje.destinoNombre(), viaje.fechaSalida(),
                viaje.cambioBus(), viaje.busPlacaAnterior(), viaje.busPlaca(),
                viaje.cambioChofer(), viaje.choferAnterior(), viaje.choferNombre());
    }
}
