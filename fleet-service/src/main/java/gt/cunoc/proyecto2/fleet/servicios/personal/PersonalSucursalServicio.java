package gt.cunoc.proyecto2.fleet.servicios.personal;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.dtos.cuentas.DatosCuentaSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.personal.DesactivarPersonalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.personal.NuevoPersonalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.personal.PersonalSucursalRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.personal.TrasladarPersonalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.personal.TrasladoPersonalRespuesta;
import gt.cunoc.proyecto2.fleet.modelos.personal.PersonalSucursalEntidad;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface PersonalSucursalServicio {

    /** F6. rol y activo opcionales. 403 SUCURSAL_AJENA, 404 SUCURSAL_NO_EXISTE */
    List<PersonalSucursalEntidad> listar(UUID sucursalId, RolPersonal rol, Boolean activo, UsuarioActual usuario);

    /** 404 PERSONAL_NO_EXISTE */
    PersonalSucursalEntidad obtenerPorId(UUID id);

    /** F7. 403 SOLO_CAJEROS, 404 SUCURSAL_NO_EXISTE, 422 SUCURSAL_INACTIVA, 409/422 de R5, 503 */
    PersonalSucursalRespuesta agregar(UUID sucursalId, NuevoPersonalSolicitud solicitud, UsuarioActual usuario);

 
    PersonalSucursalEntidad registrarAlta(SucursalEntidad sucursal, RolPersonal rol, LocalDate fechaAsignacion,
                                          DatosCuentaSolicitud cuenta);

    /** F8. 403 SOLO_CAJEROS, 409 MODIFICACION_CONCURRENTE, 422 PERSONAL_INACTIVO o SUCURSAL_SIN_ADMIN */
    PersonalSucursalRespuesta desactivar(UUID id, DesactivarPersonalSolicitud solicitud, UsuarioActual usuario);

    /** F9. 409 MODIFICACION_CONCURRENTE, 422 PERSONAL_INACTIVO, TRASLADO_MISMA_SUCURSAL, SUCURSAL_INACTIVA o SUCURSAL_SIN_ADMIN */
    TrasladoPersonalRespuesta trasladar(UUID id, TrasladarPersonalSolicitud solicitud);
}