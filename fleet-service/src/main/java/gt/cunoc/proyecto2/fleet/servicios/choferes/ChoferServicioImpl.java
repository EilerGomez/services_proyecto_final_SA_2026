package gt.cunoc.proyecto2.fleet.servicios.choferes;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.fleet.clientes.identity.ClienteIdentity;
import gt.cunoc.proyecto2.fleet.clientes.trips.ClienteTrips;
import gt.cunoc.proyecto2.fleet.dtos.choferes.ActualizarChoferSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.choferes.ActualizarLicenciaSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.choferes.ChoferInternoRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.choferes.ChoferRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.choferes.DesactivarChoferSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.choferes.NuevoChoferSolicitud;
import gt.cunoc.proyecto2.fleet.dtos.identity.CuentaPersonalRespuesta;
import gt.cunoc.proyecto2.fleet.dtos.identity.SolicitudCuentaPersonal;
import gt.cunoc.proyecto2.fleet.dtos.trips.ViajesActivosRespuesta;
import gt.cunoc.proyecto2.fleet.errores.choferes.ErrorChofer;
import gt.cunoc.proyecto2.fleet.errores.generales.ErrorGeneral;
import gt.cunoc.proyecto2.fleet.errores.sucursales.ErrorSucursal;
import gt.cunoc.proyecto2.fleet.eventos.PublicadorEventosFleet;
import gt.cunoc.proyecto2.fleet.modelos.choferes.ChoferEntidad;
import gt.cunoc.proyecto2.fleet.modelos.choferes.EstadoChofer;
import gt.cunoc.proyecto2.fleet.modelos.sucursales.SucursalEntidad;
import gt.cunoc.proyecto2.fleet.repositorios.choferes.ChoferRepositorio;
import gt.cunoc.proyecto2.fleet.repositorios.sucursales.SucursalRepositorio;
import gt.cunoc.proyecto2.fleet.servicios.fotos.AlmacenFotosServicio;
import gt.cunoc.proyecto2.fleet.utilidades.Fechas;
import gt.cunoc.proyecto2.fleet.utilidades.FiltroSucursal;
import gt.cunoc.proyecto2.fleet.utilidades.VerificadorVersion;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChoferServicioImpl implements ChoferServicio {

    private final ChoferRepositorio repositorio;
    private final SucursalRepositorio sucursales;
    private final AlmacenFotosServicio fotos;
    private final ClienteIdentity identity;
    private final ClienteTrips trips;
    private final PublicadorEventosFleet eventos;

    public ChoferServicioImpl(ChoferRepositorio repositorio, SucursalRepositorio sucursales,
                              AlmacenFotosServicio fotos, ClienteIdentity identity, ClienteTrips trips,
                              PublicadorEventosFleet eventos) {
        this.repositorio = repositorio;
        this.sucursales = sucursales;
        this.fotos = fotos;
        this.identity = identity;
        this.trips = trips;
        this.eventos = eventos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChoferEntidad> listar(UUID sucursalId, EstadoChofer estado, UsuarioActual usuario) {
        return repositorio.buscar(FiltroSucursal.resolver(usuario, sucursalId), estado);
    }

    @Override
    @Transactional(readOnly = true)
    public ChoferEntidad obtenerPorId(UUID id) {
        return repositorio.findById(id)
                .orElseThrow(() -> ErrorChofer.CHOFER_NO_EXISTE.excepcion());
    }

    @Override
    @Transactional(readOnly = true)
    public ChoferEntidad obtener(UUID id, UsuarioActual usuario) {
        ChoferEntidad chofer = obtenerPorId(id);
        usuario.verificarAccesoSucursal(chofer.getSucursalId());
        return chofer;
    }

    @Override
    @Transactional(readOnly = true)
    public ChoferEntidad obtenerPropio(UsuarioActual usuario) {
        if (usuario.choferId() == null) {
            throw ErrorChofer.CHOFER_NO_EXISTE.excepcion();
        }
        return obtenerPorId(usuario.choferId());
    }

    @Override
    @Transactional
    public ChoferRespuesta crear(NuevoChoferSolicitud solicitud, UsuarioActual usuario) {
        usuario.verificarAccesoSucursal(solicitud.sucursalId());

        // El id del chofer es el solicitudId de R5: el mismo en cada reintento
        UUID choferId = solicitud.cuenta().solicitudId() != null
                ? solicitud.cuenta().solicitudId() : UUID.randomUUID();
        
        Optional<ChoferEntidad> yaRegistrado = repositorio.findById(choferId);
        
        if (yaRegistrado.isPresent()) {
            return reintentoDeAltaCompleta(yaRegistrado.get(), solicitud);
        }

        verificarSucursalActiva(solicitud.sucursalId());
        
        fotos.verificarUrl(solicitud.cuenta().fotoUrl());
        
        verificarLicenciaVigente(solicitud.fechaVencimientoLicencia(), solicitud.fechaRegistro());
        
        if (repositorio.existsByNumeroLicencia(solicitud.numeroLicencia())) {
            throw ErrorChofer.LICENCIA_EN_USO.excepcion(solicitud.numeroLicencia());
        }

        CuentaPersonalRespuesta cuenta = identity.crearCuentaPersonal(
                SolicitudCuentaPersonal.paraChofer(choferId, solicitud.sucursalId(), solicitud.cuenta()));

        ChoferEntidad chofer = repositorio.saveAndFlush(
                solicitud.crearEntidad(choferId, cuenta.usuarioId(), cuenta.nombreCompleto()));
        
        eventos.choferCreado(chofer, solicitud.fechaRegistro());
        
        eventos.licenciaChoferActualizada(chofer, solicitud.fechaRegistro());   // Notification programa los recordatorios

        return new ChoferRespuesta(chofer);
    }

    @Override
    @Transactional
    public ChoferRespuesta actualizar(UUID id, ActualizarChoferSolicitud solicitud, UsuarioActual usuario) {
        ChoferEntidad chofer = obtener(id, usuario);
        VerificadorVersion.verificar(chofer.getVersion(), solicitud.version());

        chofer.actualizarSalario(solicitud.salarioBaseViaje());
        chofer = repositorio.saveAndFlush(chofer);
        eventos.choferActualizado(chofer, Fechas.hoy());

        return new ChoferRespuesta(chofer);
    }

    @Override
    @Transactional
    public ChoferRespuesta actualizarLicencia(UUID id, ActualizarLicenciaSolicitud solicitud,
                                              UsuarioActual usuario) {
        ChoferEntidad chofer = obtener(id, usuario);
        VerificadorVersion.verificar(chofer.getVersion(), solicitud.version());
        verificarLicenciaVigente(solicitud.fechaVencimientoLicencia(), solicitud.fechaRegistro());
        if (repositorio.existsByNumeroLicenciaAndIdNot(solicitud.numeroLicencia(), id)) {
            throw ErrorChofer.LICENCIA_EN_USO.excepcion(solicitud.numeroLicencia());
        }

        solicitud.aplicarA(chofer);
        chofer = repositorio.saveAndFlush(chofer);
        eventos.choferActualizado(chofer, solicitud.fechaRegistro());
        eventos.licenciaChoferActualizada(chofer, solicitud.fechaRegistro());   // Notification reprograma los recordatorios

        return new ChoferRespuesta(chofer);
    }

    @Override
    @Transactional
    public ChoferRespuesta desactivar(UUID id, DesactivarChoferSolicitud solicitud, UsuarioActual usuario) {
        ChoferEntidad chofer = obtener(id, usuario);
        VerificadorVersion.verificar(chofer.getVersion(), solicitud.version());
        if (!chofer.esActivo()) {
            throw ErrorChofer.CHOFER_INACTIVO.excepcion();
        }

        ViajesActivosRespuesta viajes = trips.viajesActivosDelChofer(id);       // R3
        if (viajes.hayViajes()) {
            throw ErrorChofer.CHOFER_CON_VIAJES_ACTIVOS.excepcion(viajes.cantidad());
        }

        chofer.desactivar();
        chofer = repositorio.saveAndFlush(chofer);
        eventos.choferDesactivado(chofer, solicitud.fecha());                    // Identity le quita el rol CHOFER

        return new ChoferRespuesta(chofer);
    }

    @Override
    @Transactional
    public void actualizarNombreDesdePerfil(UUID choferId, String nombreCompleto, LocalDate fechaNegocio) {
        Optional<ChoferEntidad> encontrado = repositorio.findById(choferId);
        if (encontrado.isEmpty() || !encontrado.get().actualizarNombre(nombreCompleto)) {
            return;                                         // no existe o el nombre no cambió: sin evento
        }
        ChoferEntidad chofer = repositorio.saveAndFlush(encontrado.get());
        eventos.choferActualizado(chofer, fechaNegocio);    // Trips y Reporting reciben el nombre nuevo
    }

    @Override
    @Transactional(readOnly = true)
    public ChoferInternoRespuesta consultarParaViaje(UUID id, LocalDate fecha) {
        return new ChoferInternoRespuesta(obtenerPorId(id), fecha);
    }

    private void verificarSucursalActiva(UUID sucursalId) {
        SucursalEntidad sucursal = sucursales.findById(sucursalId)
                .orElseThrow(() -> ErrorSucursal.SUCURSAL_NO_EXISTE.excepcion());
        if (!sucursal.isActiva()) {
            throw ErrorSucursal.SUCURSAL_INACTIVA.excepcion();
        }
    }

    /** No se registra una licencia que ya venció en la fecha de registro elegida por el usuario. */
    private static void verificarLicenciaVigente(LocalDate vencimiento, LocalDate fechaRegistro) {
        if (vencimiento.isBefore(fechaRegistro)) {
            throw ErrorChofer.LICENCIA_VENCIDA.excepcion(vencimiento, fechaRegistro);
        }
    }

    /** El frontend reintentó un alta que sí se guardó (se perdió la respuesta): se devuelve el mismo chofer. */
    private static ChoferRespuesta reintentoDeAltaCompleta(ChoferEntidad existente, NuevoChoferSolicitud solicitud) {
        if (!existente.getSucursalId().equals(solicitud.sucursalId())
                || !existente.getNumeroLicencia().equals(solicitud.numeroLicencia())) {
            throw ErrorGeneral.SOLICITUD_YA_USADA.excepcion();
        }
        return new ChoferRespuesta(existente);
    }
}