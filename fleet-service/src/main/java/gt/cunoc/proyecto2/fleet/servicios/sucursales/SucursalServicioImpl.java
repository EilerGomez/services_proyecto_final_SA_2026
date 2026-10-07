package gt.cunoc.proyecto2.fleet.servicios.sucursales;

import gt.cunoc.proyecto2.fleet.dtos.sucursales.ActualizarSucursalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.CambiarEstadoSucursalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.personal.PersonalSucursalRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.NuevaSucursalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.SucursalCreadaRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.SucursalRespuesta;
import gt.cunoc.proyecto2.fleet.errores.sucursales.ErrorSucursal;
import gt.cunoc.proyecto2.fleet.modelos.personal.PersonalSucursalEntidad;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import gt.cunoc.proyecto2.fleet.repositorios.sucursales.SucursalRepositorio;
import gt.cunoc.proyecto2.fleet.servicios.personal.PersonalSucursalServicio;
import gt.cunoc.proyecto2.fleet.utilidades.VerificadorVersion;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SucursalServicioImpl implements SucursalServicio {

    private final SucursalRepositorio repositorio;
    private final PersonalSucursalServicio personal;

    public SucursalServicioImpl(SucursalRepositorio repositorio, PersonalSucursalServicio personal) {
        this.repositorio = repositorio;
        this.personal = personal;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SucursalEntidad> listar(Boolean activa) {
        if (activa == null) {
            return repositorio.findAllByOrderByNombreAsc();
        }
        return repositorio.findByActivaOrderByNombreAsc(activa);
    }

    @Override
    @Transactional(readOnly = true)
    public SucursalEntidad obtenerPorId(UUID id) {
        return repositorio.findById(id)
                .orElseThrow(() -> ErrorSucursal.SUCURSAL_NO_EXISTE.excepcion());
    }

    @Override
    @Transactional
    public SucursalCreadaRespuesta crear(NuevaSucursalSolicitud solicitud) {
        if (repositorio.existsByCodigo(solicitud.codigo())) {
            throw ErrorSucursal.SUCURSAL_DUPLICADA.excepcion("código", solicitud.codigo());
        }
        if (repositorio.existsByNombre(solicitud.nombre())) {
            throw ErrorSucursal.SUCURSAL_DUPLICADA.excepcion("nombre", solicitud.nombre());
        }

        // saveAndFlush: la sucursal ya existe en la BD (FK del admin) y su version (0) queda asignada
        SucursalEntidad sucursal = repositorio.saveAndFlush(solicitud.crearEntidad());

        // Aquí se publica el evento SucursalCreada
        // (aggregateVersion = sucursal.getVersion(), businessDate = fechaAsignacion).

        // R5 + INSERT del admin (también publica PersonalSucursalAsignado).
        // Si R5 falla, se revierte toda la transacción y la sucursal no queda guardada.
        PersonalSucursalEntidad admin = personal.registrarAlta(sucursal, RolPersonal.ADMIN_SUCURSAL,
                solicitud.fechaAsignacion(), solicitud.adminInicial());

        return new SucursalCreadaRespuesta(new SucursalRespuesta(sucursal), new PersonalSucursalRespuesta(admin));
    }

    @Override
    @Transactional
    public SucursalRespuesta actualizar(UUID id, ActualizarSucursalSolicitud solicitud) {
        SucursalEntidad sucursal = obtenerPorId(id);
        VerificadorVersion.verificar(sucursal.getVersion(), solicitud.version());

        if (repositorio.existsByNombreAndIdNot(solicitud.nombre(), id)) {
            throw ErrorSucursal.SUCURSAL_DUPLICADA.excepcion("nombre", solicitud.nombre());
        }

        solicitud.aplicarA(sucursal);
        sucursal = repositorio.saveAndFlush(sucursal);

        // Aquí se publica el evento SucursalActualizada (aggregateVersion = sucursal.getVersion()).

        return new SucursalRespuesta(sucursal);
    }

    @Override
    @Transactional
    public SucursalRespuesta cambiarEstado(UUID id, CambiarEstadoSucursalSolicitud solicitud) {
        SucursalEntidad sucursal = obtenerPorId(id);
        VerificadorVersion.verificar(sucursal.getVersion(), solicitud.version());

        if (sucursal.isActiva() == solicitud.activa()) {
            return new SucursalRespuesta(sucursal);          // ya está en ese estado: no hay cambio ni evento
        }

        if (!solicitud.activa()) {
            // Paso buses: si la sucursal tiene buses en estado distinto de INACTIVO
            // -> 422 SUCURSAL_CON_BUSES_ACTIVOS.
        }

        sucursal.setActiva(solicitud.activa());
        sucursal = repositorio.saveAndFlush(sucursal);

        // Aquí se publica el evento SucursalActualizada (aggregateVersion = sucursal.getVersion()).

        return new SucursalRespuesta(sucursal);
    }
}