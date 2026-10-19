package gt.cunoc.proyecto2.fleet.servicios.sucursales;

import gt.cunoc.proyecto2.fleet.dtos.sucursales.ActualizarSucursalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.CambiarEstadoSucursalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.personal.PersonalSucursalRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.NuevaSucursalSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.SucursalCreadaRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.sucursales.SucursalRespuesta;
import gt.cunoc.proyecto2.fleet.errores.sucursales.ErrorSucursal;
import gt.cunoc.proyecto2.fleet.eventos.PublicadorEventosFleet;
import gt.cunoc.proyecto2.fleet.modelos.buses.EstadoBus;
import gt.cunoc.proyecto2.fleet.modelos.personal.PersonalSucursalEntidad;
import gt.cunoc.proyecto2.fleet.modelos.personal.RolPersonal;
import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import gt.cunoc.proyecto2.fleet.repositorios.buses.BusRepositorio;
import gt.cunoc.proyecto2.fleet.repositorios.sucursales.SucursalRepositorio;
import gt.cunoc.proyecto2.fleet.servicios.personal.PersonalSucursalServicio;
import gt.cunoc.proyecto2.fleet.utilidades.Fechas;
import gt.cunoc.proyecto2.fleet.utilidades.VerificadorVersion;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SucursalServicioImpl implements SucursalServicio {

    private final SucursalRepositorio repositorio;
    private final PersonalSucursalServicio personal;
    private final BusRepositorio buses;
    private final PublicadorEventosFleet eventos;

    public SucursalServicioImpl(SucursalRepositorio repositorio, PersonalSucursalServicio personal,
                                BusRepositorio buses, PublicadorEventosFleet eventos) {
        this.repositorio = repositorio;
        this.personal = personal;
        this.buses = buses;
        this.eventos = eventos;
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
        boolean adminExistente = solicitud.conAdminExistente();     // 400 si no viene exactamente uno
        if (repositorio.existsByCodigo(solicitud.codigo())) {
            throw ErrorSucursal.SUCURSAL_DUPLICADA.excepcion("código", solicitud.codigo());
        }
        if (repositorio.existsByNombre(solicitud.nombre())) {
            throw ErrorSucursal.SUCURSAL_DUPLICADA.excepcion("nombre", solicitud.nombre());
        }

        // saveAndFlush: la sucursal ya existe en la BD (FK del admin) y su version (0) queda asignada
        SucursalEntidad sucursal = repositorio.saveAndFlush(solicitud.crearEntidad());

        eventos.sucursalCreada(sucursal, solicitud.fechaAsignacion());

        // R5 (cuenta nueva) o R5b (cuenta existente) + INSERT del admin (también publica PersonalSucursalAsignado).
        // Si Identity rechaza, se revierte toda la transacción y la sucursal no queda guardada.
        PersonalSucursalEntidad admin = adminExistente
                ? personal.registrarAltaExistente(sucursal, RolPersonal.ADMIN_SUCURSAL,
                        solicitud.fechaAsignacion(), solicitud.adminExistente())
                : personal.registrarAlta(sucursal, RolPersonal.ADMIN_SUCURSAL,
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
        eventos.sucursalActualizada(sucursal, Fechas.hoy());

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

        if (!solicitud.activa() && buses.existsBySucursalIdAndEstadoNot(id, EstadoBus.INACTIVO)) {
            throw ErrorSucursal.SUCURSAL_CON_BUSES_ACTIVOS.excepcion();
        }

        sucursal.setActiva(solicitud.activa());
        sucursal = repositorio.saveAndFlush(sucursal);
        eventos.sucursalActualizada(sucursal, Fechas.hoy());

        return new SucursalRespuesta(sucursal);
    }
}