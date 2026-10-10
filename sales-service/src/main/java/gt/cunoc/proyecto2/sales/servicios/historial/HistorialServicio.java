package gt.cunoc.proyecto2.sales.servicios.historial;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.dtos.historial.HistorialRespuesta;

public interface HistorialServicio {

    HistorialRespuesta delCliente(UsuarioActual usuario);

    HistorialRespuesta porNit(String nit);
}