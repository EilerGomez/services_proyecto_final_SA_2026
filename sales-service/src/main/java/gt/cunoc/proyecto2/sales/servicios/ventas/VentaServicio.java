package gt.cunoc.proyecto2.sales.servicios.ventas;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.dtos.ventas.CancelarVentaSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.FiltroVentas;
import gt.cunoc.proyecto2.sales.dtos.ventas.NuevaVentaEnLineaSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.NuevaVentaMostradorSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.ReembolsoVentaSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.VentaPendienteRespuesta;
import gt.cunoc.proyecto2.sales.dtos.ventas.VentaRespuesta;
import java.util.List;
import java.util.UUID;

/** Venta de boletos: en línea (saga con la cartera), en mostrador, consulta, listado y cancelación. */
public interface VentaServicio {

    /**
     * S4 · Venta en mostrador, en efectivo y CONFIRMADA al instante.
     */
    VentaRespuesta venderEnMostrador(NuevaVentaMostradorSolicitud solicitud, UsuarioActual usuario);

    /*
     * S2 Venta en línea: reserva los asientos unos minutos y queda PENDIENTE_PAGO mientras Wallet cobra
     * la cartera (publica DebitoCarteraSolicitado; la respuesta llega a PagoCarteraServicio).
     */
    VentaPendienteRespuesta venderEnLinea(NuevaVentaEnLineaSolicitud solicitud, UsuarioActual usuario);

    /*
     * S5 CONFIRMADA → CANCELADA: boletos CANCELADO y asientos DISPONIBLE. Publica VentaCancelada y, si se
     * pagó con cartera, ReembolsoCarteraSolicitado. Admin de la sucursal de la venta (403), version (409),
     * 422 ESTADO_INVALIDO, 422 VIAJE_YA_INICIO, 400 FECHA_CANCELACION_INVALIDA.
     */
    VentaRespuesta cancelar(UUID id, CancelarVentaSolicitud solicitud, UsuarioActual usuario);

    /*
     *Devolución del dinero de una venta cancelada, desde la sucursal que la atendió.
     */
    VentaRespuesta reembolsar(UUID id, ReembolsoVentaSolicitud solicitud, UsuarioActual usuario);

    /*
     * Venta cuyo boleto impreso lleva este código de barras (los primeros 12 caracteres del id)
     */
    List<VentaRespuesta> buscarPorCodigo(String codigo, UsuarioActual usuario);

    /*
     * ViajeEliminado (compensación): ventas PENDIENTE_PAGO → RECHAZADA (VIAJE_ELIMINADO); ventas CONFIRMADA →
     * CANCELADA con el motivo de Trips, VentaCancelada y, si se pagaron con cartera, el reembolso
     */
    void compensarViajeEliminado(UUID viajeId, String motivo);

    /*
     * Tarea programada: rechaza (TIEMPO_AGOTADO) las ventas en línea cuya reserva venció y libera sus asientos.
     * Devuelve cuántas ventas rechazó.
     */
    int liberarReservasVencidas();

    /*el cliente dueño, el personal de su sucursal o ADMIN_SISTEMA (403 VENTA_AJENA). 404 VENTA_NO_EXISTE */
    VentaRespuesta obtener(UUID id, UsuarioActual usuario);

    /*el filtro ya viene ajustado a quién busca. Las más recientes primero. */
    List<VentaRespuesta> listar(FiltroVentas filtro);
}
