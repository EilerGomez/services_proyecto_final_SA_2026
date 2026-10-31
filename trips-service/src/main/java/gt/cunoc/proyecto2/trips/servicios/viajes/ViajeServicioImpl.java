package gt.cunoc.proyecto2.trips.servicios.viajes;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.dtos.operaciones.LlegadaRespuesta;
import gt.cunoc.proyecto2.trips.dtos.operaciones.SalidaRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ActualizarViajeSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.FiltroViajes;
import gt.cunoc.proyecto2.trips.dtos.viajes.NombresViaje;
import gt.cunoc.proyecto2.trips.dtos.viajes.NuevoViajeSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeRegresoSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.VendibleRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeDetalleRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajesActivosRespuesta;
import gt.cunoc.proyecto2.trips.errores.viajes.ErrorViaje;
import gt.cunoc.proyecto2.trips.eventos.PublicadorEventosTrips;
import gt.cunoc.proyecto2.trips.eventos.publicados.viajes.ViajeReasignado;
import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.buses.VistaBusEntidad;
import gt.cunoc.proyecto2.trips.modelos.choferes.VistaChoferEntidad;
import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.AsignacionViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.repositorios.alquileres.AlquilerPrivadoRepositorio;
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

    /**
     * Listados de gestión: primero lo último programado o modificado. La fecha de salida es el
     * desempate, para que dos viajes tocados en el mismo instante siempre salgan en el mismo orden.
     */
    /** Estado de un chofer vigente en vista_choferes (lo escribe el manejador de eventos de Flota). */
    private static final String CHOFER_ACTIVO = "ACTIVO";

    private static final Sort RECIENTES = Sort.by(Sort.Direction.DESC, "fechaActualizacion", "fechaSalidaProgramada");

    private final ViajeRepositorio repositorio;
    private final RutaServicio rutas;
    private final RutaRepositorio rutaRepositorio;
    private final VistaSucursalServicio sucursales;
    private final VistaBusRepositorio buses;
    private final VistaChoferRepositorio choferes;
    private final AsignacionViajeServicio asignaciones;
    private final SalidaViajeRepositorio salidas;
    private final LlegadaViajeRepositorio llegadas;
    private final AlquilerPrivadoRepositorio alquileres;
    private final PublicadorEventosTrips eventos;

    public ViajeServicioImpl(ViajeRepositorio repositorio, RutaServicio rutas, RutaRepositorio rutaRepositorio,
                             VistaSucursalServicio sucursales, VistaBusRepositorio buses,
                             VistaChoferRepositorio choferes, AsignacionViajeServicio asignaciones,
                             SalidaViajeRepositorio salidas, LlegadaViajeRepositorio llegadas,
                             AlquilerPrivadoRepositorio alquileres, PublicadorEventosTrips eventos) {
        this.repositorio = repositorio;
        this.rutas = rutas;
        this.rutaRepositorio = rutaRepositorio;
        this.sucursales = sucursales;
        this.buses = buses;
        this.choferes = choferes;
        this.asignaciones = asignaciones;
        this.salidas = salidas;
        this.llegadas = llegadas;
        this.alquileres = alquileres;
        this.eventos = eventos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ViajeRespuesta> buscar(FiltroViajes filtro) {
        return respuestas(repositorio.findAll(EspecificacionesViaje.filtrar(filtro), RECIENTES));
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
    public ViajeRespuesta resumen(ViajeEntidad viaje) {
        return respuesta(viaje);
    }

    @Override
    @Transactional(readOnly = true)
    public ViajeEntidad obtenerPorId(UUID id) {
        return repositorio.findById(id)
                .orElseThrow(() -> ErrorViaje.VIAJE_NO_EXISTE.excepcion());
    }

    /**
     * T10. Programa el viaje de ida y, si la solicitud lo pide, también el de vuelta por la misma
     * ruta. Los dos van en la misma transacción: si el regreso no pasa las validaciones (el bus o el
     * piloto ya ocupados, o un horario que se traslapa con su propia ida) no se crea ninguno de los
     * dos, que es lo que espera quien llenó el formulario completo.
     *
     * <p>La ida se guarda antes de validar el regreso a propósito: así la comprobación de choques
     * del regreso la ve y rechaza un bus que todavía viene de camino.</p>
     */
    @Override
    @Transactional
    public ViajeRespuesta programar(NuevoViajeSolicitud solicitud, UsuarioActual usuario) {
        RutaEntidad ruta = rutas.obtenerPorId(solicitud.rutaId());
        usuario.verificarAccesoSucursal(ruta.getSucursalOrigenId());
        if (!ruta.isActiva()) {
            throw ErrorViaje.RUTA_INACTIVA.excepcion();
        }
        // El regreso va en el mismo bus que la ida: se revisa antes de crear nada, para no dejar la
        // ida programada sola por un dato que el formulario puede corregir.
        if (solicitud.pidioRegreso() && !solicitud.busId().equals(solicitud.regreso().busId())) {
            throw ErrorViaje.REGRESO_CON_OTRO_BUS.excepcion();                          // 422
        }
        AsignacionViaje asignacion = asignaciones.validarNueva(ruta.getSucursalOrigenId(), solicitud.busId(),
                solicitud.choferId(), solicitud.fechaSalidaProgramada(), solicitud.fechaLlegadaEstimada());
        ViajeEntidad viaje = repositorio.saveAndFlush(solicitud.crearEntidad(ruta, asignacion, usuario.usuarioId()));
        ViajeRespuesta respuesta = respuesta(viaje);
        eventos.viajeProgramado(respuesta);

        if (solicitud.pidioRegreso()) {
            programarRegreso(ruta, solicitud.regreso(), viaje, usuario);
        }
        return respuesta;
    }

    /**
     * El viaje de vuelta: misma ruta, sentido REGRESO. No se le cambia el origen ni el destino en la
     * base —son los de la ruta— porque quien invierte el recorrido al mostrarlo es NombresViaje.
     *
     * <p>Va en el mismo bus que la ida: la unidad que se fue es la que se regresa. El piloto sí puede
     * ser otro (el relevo que espera en la otra punta).</p>
     */
    private void programarRegreso(RutaEntidad ruta, ViajeRegresoSolicitud regreso, ViajeEntidad ida,
                                  UsuarioActual usuario) {
        if (!regreso.fechaSalidaProgramada().isAfter(ida.getFechaSalidaProgramada())) {
            throw ErrorViaje.REGRESO_ANTES_DE_LA_IDA.excepcion();                       // 400
        }
        AsignacionViaje asignacion = asignaciones.validarNueva(ruta.getSucursalOrigenId(), regreso.busId(),
                regreso.choferId(), regreso.fechaSalidaProgramada(), regreso.fechaLlegadaEstimada());
        ViajeEntidad vuelta = repositorio.saveAndFlush(ViajeEntidad.programarRegreso(ruta, asignacion,
                usuario.usuarioId(), regreso.ventaPorChoferActiva(), ida));
        eventos.viajeProgramado(respuesta(vuelta));
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
        ViajeRespuesta antes = respuesta(viaje);   // placa y piloto de ahora, para contar el cambio
        viaje.reprogramar(asignacion);
        viaje.verificarMismoBusQueLaIda(busDeLaIda(viaje));   // el regreso no cambia de bus
        if (solicitud.ventaPorChofer() != null) {
            viaje.permitirVentaPorChofer(solicitud.ventaPorChofer());
        }
        ViajeRespuesta respuesta = respuesta(repositorio.saveAndFlush(viaje));
        eventos.viajeActualizado(respuesta);
        avisarSiCambioLaUnidad(viaje, antes, respuesta);
        return respuesta;
    }

    /** El bus del viaje de ida de este regreso, o null si no es un regreso o su ida ya no existe. */
    private UUID busDeLaIda(ViajeEntidad viaje) {
        if (!viaje.esRegreso() || viaje.getViajeIdaId() == null) {
            return null;
        }
        return repositorio.findById(viaje.getViajeIdaId()).map(ViajeEntidad::getBusId).orElse(null);
    }

    /**
     * Cambiar la hora es una cosa y cambiar el bus o el piloto es otra: quien ya tiene el boleto o
     * contrató el alquiler buscará otra unidad en la terminal. Solo en ese caso sale el aviso.
     */
    private void avisarSiCambioLaUnidad(ViajeEntidad viaje, ViajeRespuesta antes, ViajeRespuesta ahora) {
        if (!ViajeReasignado.huboCambio(antes, ahora)) {
            return;
        }
        AlquilerPrivadoEntidad alquiler = viaje.esPrivado() && viaje.getAlquilerId() != null
                ? alquileres.findById(viaje.getAlquilerId()).orElse(null)
                : null;
        eventos.viajeReasignado(ahora, ViajeReasignado.desde(antes, ahora, alquiler));
    }

    @Override
    @Transactional
    public void eliminar(UUID id, String motivo, UsuarioActual usuario) {
        ViajeEntidad viaje = obtenerPorId(id);
        usuario.verificarAccesoSucursal(viaje.getSucursalId());
        viaje.verificarProgramado();                                  // el trigger también lo impide
        repositorio.delete(viaje);
        repositorio.flush();
        if (viaje.esPrivado()) {                                      // el alquiler vuelve a PAGADO: otro bus
            alquileres.findById(viaje.getAlquilerId()).ifPresent(alquiler -> {
                alquiler.liberar();
                alquileres.saveAndFlush(alquiler);
            });
        }
        eventos.viajeEliminado(viaje, motivo);                       // version + 1
    }

    @Override
    @Transactional
    public UUID eliminarPorCancelacionDeAlquiler(UUID alquilerId, String motivo) {
        List<ViajeEntidad> suyos = repositorio.findByAlquilerIdIn(List.of(alquilerId));
        if (suyos.isEmpty()) {
            return null;                                              // el alquiler no llegó a ASIGNADO
        }
        ViajeEntidad viaje = suyos.get(0);
        viaje.verificarProgramado();                                  // 422 si ya salió (el trigger también)
        repositorio.delete(viaje);
        repositorio.flush();
        // No se llama a alquiler.liberar(): el alquiler queda CANCELADO, no vuelve a PAGADO
        eventos.viajeEliminado(viaje, motivo);                        // Sales y Reporting borran su copia
        return viaje.getId();
    }

    @Override
    @Transactional(readOnly = true)
    public VendibleRespuesta consultarVendible(UUID id) {
        return repositorio.findById(id)
                .map(viaje -> new VendibleRespuesta(viaje, choferActivo(viaje.getChoferId())))
                .orElseGet(() -> VendibleRespuesta.noExiste(id));
    }

    /**
     * ¿El piloto sigue ACTIVO en Flota? Se mira en la copia local (vista_choferes), que la mantiene
     * ChoferDesactivado. Un piloto dado de baja deja de poder vender aunque el viaje siga marcado.
     */
    private boolean choferActivo(UUID choferId) {
        return choferes.findById(choferId)
                .map(chofer -> CHOFER_ACTIVO.equals(chofer.getEstado()))
                .orElse(false);
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

    /*
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

    private List<ViajeRespuesta> respuestas(List<ViajeEntidad> viajes) {
        Map<UUID, RutaEntidad> rutasPorId = rutaRepositorio.findAllById(idsDe(viajes, ViajeEntidad::getRutaId))
                .stream().collect(Collectors.toMap(RutaEntidad::getId, Function.identity()));
        Set<UUID> sucursalIds = rutasPorId.values().stream()
                .flatMap(r -> Stream.of(r.getSucursalOrigenId(), r.getSucursalDestinoId()))
                .collect(Collectors.toSet());
        Map<UUID, VistaBusEntidad> busesPorId = buses.findAllById(idsDe(viajes, ViajeEntidad::getBusId)).stream()
                .collect(Collectors.toMap(VistaBusEntidad::getId, Function.identity()));
        Map<UUID, VistaChoferEntidad> choferesPorId = choferes
                .findAllById(idsDe(viajes, ViajeEntidad::getChoferId)).stream()
                .collect(Collectors.toMap(VistaChoferEntidad::getId, Function.identity()));
        Map<UUID, AlquilerPrivadoEntidad> alquileresPorId = alquileres
                .findAllById(idsDe(viajes, ViajeEntidad::getAlquilerId)).stream()
                .collect(Collectors.toMap(AlquilerPrivadoEntidad::getId, Function.identity()));
        NombresViaje nombres = new NombresViaje(rutasPorId, alquileresPorId, sucursales.nombres(sucursalIds),
                busesPorId, choferesPorId);
        return viajes.stream().map(viaje -> new ViajeRespuesta(viaje, nombres)).toList();
    }

    private static Set<UUID> idsDe(List<ViajeEntidad> viajes, Function<ViajeEntidad, UUID> campo) {
        return viajes.stream().map(campo).filter(Objects::nonNull).collect(Collectors.toSet());
    }
}