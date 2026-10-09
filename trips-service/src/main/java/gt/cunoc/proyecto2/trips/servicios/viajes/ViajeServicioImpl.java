package gt.cunoc.proyecto2.trips.servicios.viajes;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.dtos.operaciones.LlegadaRespuesta;
import gt.cunoc.proyecto2.trips.dtos.operaciones.SalidaRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ActualizarViajeSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.FiltroViajes;
import gt.cunoc.proyecto2.trips.dtos.viajes.NombresViaje;
import gt.cunoc.proyecto2.trips.dtos.viajes.NuevoViajeSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.VendibleRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeDetalleRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajesActivosRespuesta;
import gt.cunoc.proyecto2.trips.errores.viajes.ErrorViaje;
import gt.cunoc.proyecto2.trips.modelos.buses.VistaBusEntidad;
import gt.cunoc.proyecto2.trips.modelos.choferes.VistaChoferEntidad;
import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.AsignacionViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.repositorios.buses.VistaBusRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.choferes.VistaChoferRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.operaciones.LlegadaViajeRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.operaciones.SalidaViajeRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.rutas.RutaRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.viajes.EspecificacionesViaje;
import gt.cunoc.proyecto2.trips.repositorios.viajes.ViajeRepositorio;
import gt.cunoc.proyecto2.trips.servicios.rutas.RutaServicio;
import gt.cunoc.proyecto2.trips.servicios.sucursales.VistaSucursalServicio;
import gt.cunoc.proyecto2.trips.utilidades.VerificadorVersion;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ViajeServicioImpl implements ViajeServicio {

    private static final Sort POR_SALIDA = Sort.by("fechaSalidaProgramada");

    private final ViajeRepositorio repositorio;
    private final RutaServicio rutas;
    private final RutaRepositorio rutaRepositorio;
    private final VistaSucursalServicio sucursales;
    private final VistaBusRepositorio buses;
    private final VistaChoferRepositorio choferes;
    private final AsignacionViajeServicio asignaciones;
    private final SalidaViajeRepositorio salidas;
    private final LlegadaViajeRepositorio llegadas;

    public ViajeServicioImpl(ViajeRepositorio repositorio, RutaServicio rutas, RutaRepositorio rutaRepositorio,
                             VistaSucursalServicio sucursales, VistaBusRepositorio buses,
                             VistaChoferRepositorio choferes, AsignacionViajeServicio asignaciones,
                             SalidaViajeRepositorio salidas, LlegadaViajeRepositorio llegadas) {
        this.repositorio = repositorio;
        this.rutas = rutas;
        this.rutaRepositorio = rutaRepositorio;
        this.sucursales = sucursales;
        this.buses = buses;
        this.choferes = choferes;
        this.asignaciones = asignaciones;
        this.salidas = salidas;
        this.llegadas = llegadas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViajeRespuesta> buscar(FiltroViajes filtro) {
        return respuestas(repositorio.findAll(EspecificacionesViaje.filtrar(filtro), POR_SALIDA));
    }

    @Override
    @Transactional(readOnly = true)
    public ViajeDetalleRespuesta obtener(UUID id, UsuarioActual usuario) {
        ViajeEntidad viaje = obtenerPorId(id);
        verificarPuedeVer(viaje, usuario);
        return detalle(viaje);
    }

    @Override
    @Transactional(readOnly = true)
    public ViajeDetalleRespuesta detalle(ViajeEntidad viaje) {
        return new ViajeDetalleRespuesta(respuesta(viaje),
                salidas.findById(viaje.getId()).map(SalidaRespuesta::new).orElse(null),
                llegadas.findById(viaje.getId()).map(LlegadaRespuesta::new).orElse(null));
    }

    @Override
    @Transactional(readOnly = true)
    public ViajeEntidad obtenerPorId(UUID id) {
        return repositorio.findById(id)
                .orElseThrow(() -> ErrorViaje.VIAJE_NO_EXISTE.excepcion());
    }

    @Override
    @Transactional
    public ViajeRespuesta programar(NuevoViajeSolicitud solicitud, UsuarioActual usuario) {
        RutaEntidad ruta = rutas.obtenerPorId(solicitud.rutaId());
        usuario.verificarAccesoSucursal(ruta.getSucursalOrigenId());
        if (!ruta.isActiva()) {
            throw ErrorViaje.RUTA_INACTIVA.excepcion();
        }
        AsignacionViaje asignacion = asignaciones.validarNueva(ruta.getSucursalOrigenId(), solicitud.busId(),
                solicitud.choferId(), solicitud.fechaSalidaProgramada(), solicitud.fechaLlegadaEstimada());
        ViajeEntidad viaje = repositorio.saveAndFlush(solicitud.crearEntidad(ruta, asignacion, usuario.usuarioId()));
        // Paso 6: aquí se publica el evento ViajeProgramado (businessDate = día de la salida)
        return respuesta(viaje);
    }

    @Override
    @Transactional
    public ViajeRespuesta actualizar(UUID id, ActualizarViajeSolicitud solicitud, UsuarioActual usuario) {
        ViajeEntidad viaje = obtenerPorId(id);
        usuario.verificarAccesoSucursal(viaje.getSucursalId());
        viaje.verificarProgramado();            // primero: si ya salió, 422 aunque la version también cambió
        VerificadorVersion.verificar(viaje.getVersion(), solicitud.version());
        AsignacionViaje asignacion = asignaciones.validarCambio(viaje, solicitud.busId(), solicitud.choferId(),
                solicitud.fechaSalidaProgramada(), solicitud.fechaLlegadaEstimada());
        viaje.reprogramar(asignacion);
        viaje = repositorio.saveAndFlush(viaje);
        // Paso 6: aquí se publica el evento ViajeActualizado (businessDate = día de la salida)
        return respuesta(viaje);
    }

    @Override
    @Transactional
    public void eliminar(UUID id, String motivo, UsuarioActual usuario) {
        ViajeEntidad viaje = obtenerPorId(id);
        usuario.verificarAccesoSucursal(viaje.getSucursalId());
        viaje.verificarProgramado();                                  // el trigger también lo impide
        repositorio.delete(viaje);
        repositorio.flush();
        // Paso 6: aquí se publica el evento ViajeEliminado con el motivo
        //         (aggregateVersion = version + 1, leída antes del delete; businessDate = hoy)
        // Paso 5: si el viaje es PRIVADO, su alquiler vuelve a PAGADO para asignarle otro bus
    }

    @Override
    @Transactional(readOnly = true)
    public VendibleRespuesta consultarVendible(UUID id) {
        return repositorio.findById(id)
                .map(VendibleRespuesta::new)
                .orElseGet(() -> VendibleRespuesta.noExiste(id));
    }

    @Override
    @Transactional(readOnly = true)
    public ViajesActivosRespuesta viajesActivos(UUID busId, UUID choferId) {
        if ((busId == null) == (choferId == null)) {
            throw ErrorViaje.FILTRO_REQUERIDO.excepcion();
        }
        List<ViajeEntidad> activos = busId != null
                ? repositorio.findByBusIdAndEstadoInOrderByFechaSalidaProgramadaAsc(busId, EstadoViaje.ACTIVOS)
                : repositorio.findByChoferIdAndEstadoInOrderByFechaSalidaProgramadaAsc(choferId, EstadoViaje.ACTIVOS);
        return new ViajesActivosRespuesta(activos);
    }

    /**
     * Un viaje PRIVADO lo ven: ADMIN_SISTEMA, el personal (ADMIN_SUCURSAL, CAJERO) de su sucursal y el
     * chofer asignado. 403 VIAJE_PRIVADO a los demás.
     */
    private static void verificarPuedeVer(ViajeEntidad viaje, UsuarioActual usuario) {
        if (viaje.esRegular()) {
            return;
        }
        if (usuario == null || !puedeVerPrivado(viaje, usuario)) {
            throw ErrorViaje.VIAJE_PRIVADO.excepcion();
        }
    }

    private static boolean puedeVerPrivado(ViajeEntidad viaje, UsuarioActual usuario) {
        boolean personalDeLaSucursal = usuario.tieneAlgunRol(Roles.ADMIN_SUCURSAL, Roles.CAJERO)
                && viaje.getSucursalId().equals(usuario.sucursalId());
        boolean choferAsignado = viaje.getChoferId().equals(usuario.choferId());
        return usuario.esAdminSistema() || personalDeLaSucursal || choferAsignado;
    }

    private ViajeRespuesta respuesta(ViajeEntidad viaje) {
        return respuestas(List.of(viaje)).get(0);
    }

    /** Carga rutas, sucursales, placas y choferes de todos los viajes con una consulta por tabla. */
    private List<ViajeRespuesta> respuestas(List<ViajeEntidad> viajes) {
        Map<UUID, RutaEntidad> rutasPorId = rutaRepositorio.findAllById(idsDe(viajes, ViajeEntidad::getRutaId))
                .stream().collect(Collectors.toMap(RutaEntidad::getId, Function.identity()));
        Set<UUID> sucursalIds = rutasPorId.values().stream()
                .flatMap(r -> Stream.of(r.getSucursalOrigenId(), r.getSucursalDestinoId()))
                .collect(Collectors.toSet());
        Map<UUID, String> placas = buses.findAllById(idsDe(viajes, ViajeEntidad::getBusId)).stream()
                .collect(Collectors.toMap(VistaBusEntidad::getId, VistaBusEntidad::getPlaca));
        Map<UUID, String> nombresChoferes = choferes.findAllById(idsDe(viajes, ViajeEntidad::getChoferId)).stream()
                .collect(Collectors.toMap(VistaChoferEntidad::getId, VistaChoferEntidad::getNombreCompleto));
        NombresViaje nombres = new NombresViaje(rutasPorId, sucursales.nombres(sucursalIds), placas, nombresChoferes);
        return viajes.stream().map(viaje -> new ViajeRespuesta(viaje, nombres)).toList();
    }

    private static Set<UUID> idsDe(List<ViajeEntidad> viajes, Function<ViajeEntidad, UUID> campo) {
        return viajes.stream().map(campo).filter(Objects::nonNull).collect(Collectors.toSet());
    }
}