package gt.cunoc.proyecto2.sales.servicios.ventas;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.dtos.ventas.CancelarVentaSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.FiltroVentas;
import gt.cunoc.proyecto2.sales.dtos.ventas.NuevaVentaEnLineaSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.NuevaVentaMostradorSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.VentaPendienteRespuesta;
import gt.cunoc.proyecto2.sales.dtos.ventas.VentaRespuesta;
import java.util.List;
import java.util.UUID;

/** Venta de boletos: en línea (saga con la cartera), en mostrador, consulta, listado y cancelación. */
public interface VentaServicio {

    /**
     * S4 · Venta en mostrador, en efectivo y CONFIRMADA al instante.
     * <ul>
     *   <li>404 VIAJE_NO_DISPONIBLE si el viaje no está a la venta en la copia local.</li>
     *   <li>403 OTRA_SUCURSAL si el viaje no sale de la sucursal del cajero.</li>
     *   <li>400 ASIENTOS_REPETIDOS / ASIENTO_FUERA_DE_RANGO.</li>
     *   <li>R2 a Trips: 422 VIAJE_NO_VENDIBLE si Trips dice que no; 503 TRIPS_NO_DISPONIBLE si no responde.</li>
     *   <li>409 ASIENTO_OCUPADO si otro ya tomó alguno de los asientos (no se guarda nada).</li>
     * </ul>
     */
    VentaRespuesta venderEnMostrador(NuevaVentaMostradorSolicitud solicitud, UsuarioActual usuario);

    /**
     * S2 · Venta en línea: reserva los asientos unos minutos y queda PENDIENTE_PAGO mientras Identity cobra
     * la cartera (publica DebitoCarteraSolicitado; la respuesta llega a PagoCarteraServicio).
     * <ul>
     *   <li>422 CLIENTE_SIN_PERFIL si el cliente aún no está en vista_clientes.</li>
     *   <li>404, 400, 422 y 503 como S4 (sin la regla de sucursal).</li>
     *   <li>409 ASIENTO_OCUPADO si otro ya tomó alguno de los asientos (no se guarda nada).</li>
     * </ul>
     */
    VentaPendienteRespuesta venderEnLinea(NuevaVentaEnLineaSolicitud solicitud, UsuarioActual usuario);

    /**
     * S5 · CONFIRMADA → CANCELADA: boletos CANCELADO y asientos DISPONIBLE. Publica VentaCancelada y, si se
     * pagó con cartera, ReembolsoCarteraSolicitado. Admin de la sucursal de la venta (403), version (409),
     * 422 ESTADO_INVALIDO, 422 VIAJE_YA_INICIO, 400 FECHA_CANCELACION_INVALIDA.
     */
    VentaRespuesta cancelar(UUID id, CancelarVentaSolicitud solicitud, UsuarioActual usuario);

    /**
     * ViajeEliminado (compensación): ventas PENDIENTE_PAGO → RECHAZADA (VIAJE_ELIMINADO); ventas CONFIRMADA →
     * CANCELADA con el motivo de Trips, VentaCancelada y, si se pagaron con cartera, el reembolso.
     * Se llama antes de borrar el viaje de vista_viajes (para armar VentaCancelada con origen y destino).
     */
    void compensarViajeEliminado(UUID viajeId, String motivo);

    /**
     * Tarea programada: rechaza (TIEMPO_AGOTADO) las ventas en línea cuya reserva venció y libera sus asientos.
     * Devuelve cuántas ventas rechazó.
     */
    int liberarReservasVencidas();

    /** S3: el cliente dueño, el personal de su sucursal o ADMIN_SISTEMA (403 VENTA_AJENA). 404 VENTA_NO_EXISTE */
    VentaRespuesta obtener(UUID id, UsuarioActual usuario);

    /** S6: el filtro ya viene ajustado a quién busca. Las más recientes primero. */
    List<VentaRespuesta> listar(FiltroVentas filtro);
}
