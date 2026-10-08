package gt.cunoc.proyecto2.fleet.servicios.buses;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.dtos.buses.ActualizarBusSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.buses.BusRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.buses.CambiarEstadoBusSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.buses.DisponibilidadBusRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.buses.NuevoBusSolicitud;
import gt.cunoc.proyecto2.fleet.modelos.buses.BusEntidad;
import gt.cunoc.proyecto2.fleet.modelos.buses.EstadoBus;
import java.util.List;
import java.util.UUID;

public interface BusServicio {

    List<BusEntidad> listar(UUID sucursalId, EstadoBus estado, UsuarioActual usuario);

    BusEntidad obtenerPorId(UUID id);

    BusEntidad obtener(UUID id, UsuarioActual usuario);

    BusRespuesta crear(NuevoBusSolicitud solicitud, UsuarioActual usuario);

    BusRespuesta actualizar(UUID id, ActualizarBusSolicitud solicitud, UsuarioActual usuario);

    BusRespuesta cambiarEstado(UUID id, CambiarEstadoBusSolicitud solicitud, UsuarioActual usuario);

    DisponibilidadBusRespuesta consultarDisponibilidad(UUID id);
}