package gt.cunoc.proyecto2.fleet.servicios.personal;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.clientes.identity.ClienteIdentity;
import gt.cunoc.proyecto2.fleet.dtos.cuentas.DatosCuentaSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.identity.CuentaPersonalRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.identity.SolicitudCuentaPersonal;
import gt.cunoc.proyecto2.fleet.dtos.personal.DesactivarPersonalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.personal.NuevoPersonalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.personal.PersonalSucursalRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.personal.TrasladarPersonalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.personal.TrasladoPersonalRespuesta;
import gt.cunoc.proyecto2.fleet.errores.generales.ErrorGeneral;
import gt.cunoc.proyecto2.fleet.errores.personal.ErrorPersonal;
import gt.cunoc.proyecto2.fleet.errores.sucursales.ErrorSucursal;
import gt.cunoc.proyecto2.fleet.eventos.PublicadorEventosFleet;
import gt.cunoc.proyecto2.fleet.modelos.personal.PersonalSucursalEntidad;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import gt.cunoc.proyecto2.fleet.repositorios.personal.PersonalSucursalRepositorio;
import gt.cunoc.proyecto2.fleet.repositorios.sucursales.SucursalRepositorio;
import gt.cunoc.proyecto2.fleet.utilidades.VerificadorVersion;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Usa SucursalRepositorio (y no SucursalServicio) porque SucursalServicio depende de este
 * servicio para el admin inicial; así no hay dependencia circular.
 */
@Service
public class PersonalSucursalServicioImpl implements PersonalSucursalServicio {

    private final PersonalSucursalRepositorio repositorio;
    private final SucursalRepositorio sucursales;
    private final ClienteIdentity identity;
    private final PublicadorEventosFleet eventos;

    public PersonalSucursalServicioImpl(PersonalSucursalRepositorio repositorio,
                                        SucursalRepositorio sucursales,
                                        ClienteIdentity identity,
                                        PublicadorEventosFleet eventos) {
        this.repositorio = repositorio;
        this.sucursales = sucursales;
        this.identity = identity;
        this.eventos = eventos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PersonalSucursalEntidad> listar(UUID sucursalId, RolPersonal rol, Boolean activo,
                                                UsuarioActual usuario) {
        usuario.verificarAccesoSucursal(sucursalId);
        if (!sucursales.existsById(sucursalId)) {
            throw ErrorSucursal.SUCURSAL_NO_EXISTE.excepcion();
        }
        return repositorio.buscar(sucursalId, rol, activo);
    }

    @Override
    @Transactional(readOnly = true)
    public PersonalSucursalEntidad obtenerPorId(UUID id) {
        return repositorio.findById(id)
                .orElseThrow(() -> ErrorPersonal.PERSONAL_NO_EXISTE.excepcion());
    }

    @Override
    @Transactional
    public PersonalSucursalRespuesta agregar(UUID sucursalId, NuevoPersonalSolicitud solicitud,
                                             UsuarioActual usuario) {
        usuario.verificarAccesoSucursal(sucursalId);
        verificarPuedeGestionar(solicitud.rol(), usuario);

        SucursalEntidad sucursal = sucursales.findById(sucursalId)
                .orElseThrow(() -> ErrorSucursal.SUCURSAL_NO_EXISTE.excepcion());

        PersonalSucursalEntidad personal =
                registrarAlta(sucursal, solicitud.rol(), solicitud.fechaAsignacion(), solicitud.cuenta());
        return new PersonalSucursalRespuesta(personal);
    }

    @Override
    @Transactional
    public PersonalSucursalEntidad registrarAlta(SucursalEntidad sucursal, RolPersonal rol,
                                                 LocalDate fechaAsignacion, DatosCuentaSolicitud cuenta) {
        if (!sucursal.isActiva()) {
            throw ErrorSucursal.SUCURSAL_INACTIVA.excepcion();
        }

        // El id de la fila es el solicitudId de R5: el mismo en cada reintento
        UUID personalId = cuenta.solicitudId() != null ? cuenta.solicitudId() : UUID.randomUUID();

        Optional<PersonalSucursalEntidad> yaRegistrado = repositorio.findById(personalId);
        if (yaRegistrado.isPresent()) {
            return reintentoDeAltaCompleta(yaRegistrado.get(), sucursal);
        }

        CuentaPersonalRespuesta creada = identity.crearCuentaPersonal(
                SolicitudCuentaPersonal.paraPersonal(personalId, sucursal.getId(), rol, cuenta));

        PersonalSucursalEntidad personal = repositorio.saveAndFlush(PersonalSucursalEntidad.asignar(
                personalId, sucursal.getId(), creada.usuarioId(), creada.nombreCompleto(),
                creada.correo(), rol, fechaAsignacion));
        eventos.personalAsignado(personal, fechaAsignacion);

        return personal;
    }

    @Override
    @Transactional
    public PersonalSucursalRespuesta desactivar(UUID id, DesactivarPersonalSolicitud solicitud,
                                                UsuarioActual usuario) {
        PersonalSucursalEntidad personal = obtenerPorId(id);
        usuario.verificarAccesoSucursal(personal.getSucursalId());
        verificarPuedeGestionar(personal.getRol(), usuario);
        VerificadorVersion.verificar(personal.getVersion(), solicitud.version());
        verificarActivo(personal);
        verificarQueQuedeOtroAdmin(personal);

        personal.desactivar(solicitud.fechaDesasignacion());
        personal = repositorio.saveAndFlush(personal);
        eventos.personalDesactivado(personal, solicitud.fechaDesasignacion());

        return new PersonalSucursalRespuesta(personal);
    }

    @Override
    @Transactional
    public TrasladoPersonalRespuesta trasladar(UUID id, TrasladarPersonalSolicitud solicitud) {
        PersonalSucursalEntidad anterior = obtenerPorId(id);
        VerificadorVersion.verificar(anterior.getVersion(), solicitud.version());
        verificarActivo(anterior);

        if (anterior.getSucursalId().equals(solicitud.sucursalDestinoId())) {
            throw ErrorPersonal.TRASLADO_MISMA_SUCURSAL.excepcion();
        }
        SucursalEntidad destino = sucursales.findById(solicitud.sucursalDestinoId())
                .orElseThrow(() -> ErrorSucursal.SUCURSAL_NO_EXISTE.excepcion());
        if (!destino.isActiva()) {
            throw ErrorSucursal.SUCURSAL_INACTIVA.excepcion();
        }
        verificarQueQuedeOtroAdmin(anterior);

        // Sin R5: la cuenta ya existe. Identity actualiza la sucursal del usuario con los eventos.
        PersonalSucursalEntidad nueva = anterior.trasladarA(destino.getId(), solicitud.fechaTraslado());
        anterior = repositorio.saveAndFlush(anterior);   // primero: libera el índice de usuario activo
        nueva = repositorio.saveAndFlush(nueva);

        // Primero el cierre y luego la nueva asignación: Identity termina con la sucursal nueva
        eventos.personalDesactivado(anterior, solicitud.fechaTraslado());
        eventos.personalAsignado(nueva, solicitud.fechaTraslado());

        return new TrasladoPersonalRespuesta(new PersonalSucursalRespuesta(anterior),
                new PersonalSucursalRespuesta(nueva));
    }

    @Override
    @Transactional
    public int actualizarDatosDePerfil(UUID usuarioId, String nombreCompleto, String correo) {
        return repositorio.actualizarDatosDePerfil(usuarioId, nombreCompleto, correo);
    }

    /** ADMIN_SISTEMA gestiona a todos; ADMIN_SUCURSAL solo a los cajeros. */
    private static void verificarPuedeGestionar(RolPersonal rol, UsuarioActual usuario) {
        if (!usuario.esAdminSistema() && rol != RolPersonal.CAJERO) {
            throw ErrorPersonal.SOLO_CAJEROS.excepcion();
        }
    }

    private static void verificarActivo(PersonalSucursalEntidad personal) {
        if (!personal.isActivo()) {
            throw ErrorPersonal.PERSONAL_INACTIVO.excepcion();
        }
    }

    /**
     * Una sucursal activa no se queda sin admin. Se bloquea la sucursal (FOR UPDATE) para que
     * dos peticiones al mismo tiempo no desactiven a los dos últimos admins.
     * El trigger tr_ultimo_admin_sucursal hace la misma validación en la BD.
     */
    private void verificarQueQuedeOtroAdmin(PersonalSucursalEntidad personal) {
        if (!personal.esAdminSucursal()) {
            return;
        }
        SucursalEntidad sucursal = sucursales.bloquearPorId(personal.getSucursalId())
                .orElseThrow(() -> ErrorSucursal.SUCURSAL_NO_EXISTE.excepcion());
        boolean hayOtroAdmin = repositorio.existsBySucursalIdAndRolAndActivoTrueAndIdNot(
                sucursal.getId(), RolPersonal.ADMIN_SUCURSAL, personal.getId());
        if (sucursal.isActiva() && !hayOtroAdmin) {
            throw ErrorPersonal.SUCURSAL_SIN_ADMIN.excepcion();
        }
    }

    /** El frontend reintentó una alta que sí se guardó (se perdió la respuesta): se devuelve la misma. */
    private static PersonalSucursalEntidad reintentoDeAltaCompleta(PersonalSucursalEntidad existente,
                                                                   SucursalEntidad sucursal) {
        if (!existente.getSucursalId().equals(sucursal.getId())) {
            throw ErrorGeneral.SOLICITUD_YA_USADA.excepcion();
        }
        return existente;
    }
}