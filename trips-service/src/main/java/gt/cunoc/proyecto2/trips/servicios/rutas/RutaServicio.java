package gt.cunoc.proyecto2.trips.servicios.rutas;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.dtos.rutas.ActualizarRutaSolicitud;
import gt.cunoc.proyecto2.trips.dtos.rutas.NuevaRutaSolicitud;
import gt.cunoc.proyecto2.trips.dtos.rutas.RutaRespuesta;
import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import java.util.List;
import java.util.UUID;

public interface RutaServicio {

    List<RutaRespuesta> listar(UUID sucursalOrigenId, Boolean activa);

    RutaRespuesta obtener(UUID id);

    RutaEntidad obtenerPorId(UUID id);

    RutaRespuesta crear(NuevaRutaSolicitud solicitud, UsuarioActual usuario);

    RutaRespuesta actualizar(UUID id, ActualizarRutaSolicitud solicitud, UsuarioActual usuario);

    void eliminar(UUID id, UsuarioActual usuario);
}