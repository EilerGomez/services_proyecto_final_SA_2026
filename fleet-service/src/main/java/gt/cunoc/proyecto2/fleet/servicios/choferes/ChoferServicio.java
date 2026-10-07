package gt.cunoc.proyecto2.fleet.servicios.choferes;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.dtos.choferes.ActualizarChoferSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.choferes.ActualizarLicenciaSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.choferes.ChoferInternoRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.choferes.ChoferRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.choferes.DesactivarChoferSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.choferes.NuevoChoferSolicitud;
import gt.cunoc.proyecto2.fleet.modelos.choferes.ChoferEntidad;
import gt.cunoc.proyecto2.fleet.modelos.choferes.EstadoChofer;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface ChoferServicio {

    List<ChoferEntidad> listar(UUID sucursalId, EstadoChofer estado, UsuarioActual usuario);

    ChoferEntidad obtenerPorId(UUID id);

    ChoferEntidad obtener(UUID id, UsuarioActual usuario);

    ChoferEntidad obtenerPropio(UsuarioActual usuario);

    ChoferRespuesta crear(NuevoChoferSolicitud solicitud, UsuarioActual usuario);

    ChoferRespuesta actualizar(UUID id, ActualizarChoferSolicitud solicitud, UsuarioActual usuario);

    ChoferRespuesta actualizarLicencia(UUID id, ActualizarLicenciaSolicitud solicitud, UsuarioActual usuario);

    ChoferRespuesta desactivar(UUID id, DesactivarChoferSolicitud solicitud, UsuarioActual usuario);

    void actualizarNombreDesdePerfil(UUID choferId, String nombreCompleto, LocalDate fechaNegocio);

    ChoferInternoRespuesta consultarParaViaje(UUID id, LocalDate fecha);
}