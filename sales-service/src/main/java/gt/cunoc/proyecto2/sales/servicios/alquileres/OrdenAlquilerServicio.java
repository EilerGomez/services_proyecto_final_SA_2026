package gt.cunoc.proyecto2.sales.servicios.alquileres;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.dtos.alquileres.FiltroOrdenes;
import gt.cunoc.proyecto2.sales.dtos.alquileres.OrdenAlquilerRespuesta;
import gt.cunoc.proyecto2.sales.dtos.alquileres.PagoOrdenSolicitud;
import gt.cunoc.proyecto2.sales.dtos.alquileres.ReembolsoOrdenSolicitud;
import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.AlquilerCancelado;
import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.CotizacionAlquilerConfirmada;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface OrdenAlquilerServicio {

    List<OrdenAlquilerRespuesta> listar(FiltroOrdenes filtro);

    List<OrdenAlquilerRespuesta> mias(UsuarioActual usuario);

    OrdenAlquilerRespuesta obtener(UUID id, UsuarioActual usuario);

    OrdenAlquilerRespuesta pagarConCartera(UUID id, PagoOrdenSolicitud solicitud, UsuarioActual usuario);

    OrdenAlquilerRespuesta pagarEnEfectivo(UUID id, PagoOrdenSolicitud solicitud, UsuarioActual usuario);

    void registrarOrden(CotizacionAlquilerConfirmada cotizacion);

    /**
     * AlquilerCancelado (Trips): la orden pasa a CANCELADA. Si estaba PAGADA se resuelve la tarifa de
     * reembolso con los datos crudos del evento y se deja el monto a devolver; si se pagó con cartera
     * se le pide a Wallet, y si se pagó en efectivo queda pendiente para la ventanilla.
     *
     * <p>Idempotente: una orden ya cancelada o ya reembolsada ignora el evento.</p>
     */
    void registrarCancelacion(AlquilerCancelado aviso, LocalDate fechaNegocio);

    /**
     * Devolución del efectivo de un alquiler cancelado, en la sucursal. Personal de la sucursal de la
     * orden (403), version (409) y:
     *
     * <ul>
     *   <li>422 ORDEN_YA_REEMBOLSADA si el dinero ya se devolvió o no había nada que devolver.</li>
     *   <li>422 ESTADO_INVALIDO_REEMBOLSO si la orden no está CANCELADA.</li>
     *   <li>422 REEMBOLSO_NO_EN_EFECTIVO si se pagó con cartera: eso lo acredita Wallet. Se vuelve a
     *       pedir el reembolso por si el evento anterior se perdió y la orden sigue CANCELADA.</li>
     * </ul>
     */
    OrdenAlquilerRespuesta reembolsar(UUID id, ReembolsoOrdenSolicitud solicitud, UsuarioActual usuario);
}
