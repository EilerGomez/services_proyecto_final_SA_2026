package gt.cunoc.proyecto2.fleet.servicios.personal;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.dtos.cuentas.DatosCuentaSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.cuentas.UsuarioExistenteSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.personal.DesactivarPersonalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.personal.NuevoPersonalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.personal.PersonalSucursalRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.personal.ReintegrarPersonalSolicitud;
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

    /**
     * F7 con cuenta nueva (R5) o con un usuario existente (R5b). 400 CUENTA_REQUERIDA, 403 SOLO_CAJEROS,
     * 404 SUCURSAL_NO_EXISTE, 409 PERSONAL_EN_OTRA_SUCURSAL, 422 SUCURSAL_INACTIVA, 409/422 de R5/R5b, 503
     */
    PersonalSucursalRespuesta agregar(UUID sucursalId, NuevoPersonalSolicitud solicitud, UsuarioActual usuario);

    /**
     * Alta con R5 + INSERT. La usan F3 (admin inicial) y F7.
     * Debe llamarse dentro de una transacción y con la sucursal ya guardada.
     */
    PersonalSucursalEntidad registrarAlta(SucursalEntidad sucursal, RolPersonal rol, LocalDate fechaAsignacion,
                                          DatosCuentaSolicitud cuenta);

    /**
     * Alta de una persona que ya tiene cuenta: R5b + INSERT (sin crear cuenta). La usan F3 y F7.
     * 409 PERSONAL_EN_OTRA_SUCURSAL; de Identity: 409 YA_ES_PERSONAL, 422 USUARIO_NO_EXISTE,
     * CUENTA_INACTIVA, CUENTA_BLOQUEADA, ES_CHOFER o ES_ADMIN_SISTEMA.
     */
    PersonalSucursalEntidad registrarAltaExistente(SucursalEntidad sucursal, RolPersonal rol,
                                                   LocalDate fechaAsignacion, UsuarioExistenteSolicitud existente);

    /** F8. 403 SOLO_CAJEROS, 409 MODIFICACION_CONCURRENTE, 422 PERSONAL_INACTIVO o SUCURSAL_SIN_ADMIN */
    PersonalSucursalRespuesta desactivar(UUID id, DesactivarPersonalSolicitud solicitud, UsuarioActual usuario);

    /** Evento PerfilActualizado: refresca la copia de nombre y correo. Devuelve las filas actualizadas. */
    int actualizarDatosDePerfil(UUID usuarioId, String nombreCompleto, String correo);

    /** F9. 409 MODIFICACION_CONCURRENTE, 422 PERSONAL_INACTIVO, TRASLADO_MISMA_SUCURSAL, SUCURSAL_INACTIVA o SUCURSAL_SIN_ADMIN */
    TrasladoPersonalRespuesta trasladar(UUID id, TrasladarPersonalSolicitud solicitud);

    /**
     * F10. Vuelve a asignar a una persona retirada (misma cuenta, sin R5), en la misma u otra sucursal y con
     * el mismo u otro puesto. 403 SUCURSAL_AJENA o SOLO_CAJEROS, 404 PERSONAL_NO_EXISTE o SUCURSAL_NO_EXISTE,
     * 409 PERSONAL_EN_OTRA_SUCURSAL, 422 PERSONAL_ACTIVO o SUCURSAL_INACTIVA, 400 FECHA_REINTEGRO_INVALIDA.
     * La cuenta en Identity (R4c) debe estar activa y poder ser personal: 422 PERSONAL_CUENTA_INACTIVA,
     * PERSONAL_CUENTA_BLOQUEADA o PERSONAL_CUENTA_NO_ELEGIBLE,
     * 503 IDENTITY_NO_DISPONIBLE si no se puede consultar.
     */
    PersonalSucursalRespuesta reintegrar(UUID id, ReintegrarPersonalSolicitud solicitud, UsuarioActual usuario);
}