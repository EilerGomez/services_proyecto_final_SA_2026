package gt.cunoc.proyecto2.sales.servicios.alquileres;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.dtos.alquileres.FiltroOrdenes;
import gt.cunoc.proyecto2.sales.dtos.alquileres.OrdenAlquilerRespuesta;
import gt.cunoc.proyecto2.sales.dtos.alquileres.PagoOrdenSolicitud;
import java.util.List;
import java.util.UUID;

public interface OrdenAlquilerServicio {

    List<OrdenAlquilerRespuesta> listar(FiltroOrdenes filtro);

    List<OrdenAlquilerRespuesta> mias(UsuarioActual usuario);

    OrdenAlquilerRespuesta obtener(UUID id, UsuarioActual usuario);

    OrdenAlquilerRespuesta pagarConCartera(UUID id, PagoOrdenSolicitud solicitud, UsuarioActual usuario);

    OrdenAlquilerRespuesta pagarEnEfectivo(UUID id, PagoOrdenSolicitud solicitud, UsuarioActual usuario);
}