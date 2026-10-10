package gt.cunoc.proyecto2.sales.servicios.ventas;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.dtos.ventas.FiltroVentas;
import gt.cunoc.proyecto2.sales.dtos.ventas.NuevaVentaMostradorSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.VentaRespuesta;
import java.util.List;
import java.util.UUID;

public interface VentaServicio {

    VentaRespuesta venderEnMostrador(NuevaVentaMostradorSolicitud solicitud, UsuarioActual usuario);

    VentaRespuesta obtener(UUID id, UsuarioActual usuario);

    List<VentaRespuesta> listar(FiltroVentas filtro);
}