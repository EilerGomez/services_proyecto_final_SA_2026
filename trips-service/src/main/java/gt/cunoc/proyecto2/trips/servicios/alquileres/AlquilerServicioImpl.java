package gt.cunoc.proyecto2.trips.servicios.alquileres;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.dtos.alquileres.AlquilerRespuesta;
import gt.cunoc.proyecto2.trips.dtos.alquileres.AsignarAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.CancelarAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.CotizarAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.FiltroAlquileres;
import gt.cunoc.proyecto2.trips.dtos.alquileres.NuevoAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeDetalleRespuesta;
import gt.cunoc.proyecto2.trips.errores.alquileres.ErrorAlquiler;
import gt.cunoc.proyecto2.trips.eventos.PublicadorEventosTrips;
import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.alquileres.CanalAlquiler;
import gt.cunoc.proyecto2.trips.modelos.viajes.AsignacionViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.repositorios.alquileres.AlquilerPrivadoRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.alquileres.EspecificacionesAlquiler;
import gt.cunoc.proyecto2.trips.repositorios.viajes.ViajeRepositorio;
import gt.cunoc.proyecto2.trips.servicios.sucursales.VistaSucursalServicio;
import gt.cunoc.proyecto2.trips.servicios.viajes.AsignacionViajeServicio;
import gt.cunoc.proyecto2.trips.servicios.viajes.ViajeServicio;
import gt.cunoc.proyecto2.trips.utilidades.Fechas;
import gt.cunoc.proyecto2.trips.utilidades.VerificadorVersion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlquilerServicioImpl implements AlquilerServicio {

    private static final Logger log = LoggerFactory.getLogger(AlquilerServicioImpl.class);
    private static final Sort POR_SALIDA = Sort.by("fechaSalida");

    private final AlquilerPrivadoRepositorio repositorio;
    private final ConfiguracionAlquilerServicio precios;
    private final VistaSucursalServicio sucursales;
    private final AsignacionViajeServicio asignaciones;
    private final ViajeServicio viajes;
    private final ViajeRepositorio viajeRepositorio;
    private final PublicadorEventosTrips eventos;

    public AlquilerServicioImpl(AlquilerPrivadoRepositorio repositorio, ConfiguracionAlquilerServicio precios,
                                VistaSucursalServicio sucursales, AsignacionViajeServicio asignaciones,
                                ViajeServicio viajes, ViajeRepositorio viajeRepositorio,
                                PublicadorEventosTrips eventos) {
        this.repositorio = repositorio;
        this.precios = precios;
        this.sucursales = sucursales;
        this.asignaciones = asignaciones;
        this.viajes = viajes;
        this.viajeRepositorio = viajeRepositorio;
        this.eventos = eventos;
    }

    @Override
    @Transactional
    public AlquilerRespuesta solicitar(NuevoAlquilerSolicitud solicitud, UsuarioActual usuario) {
        boolean enMostrador = usuario.tieneRol(Roles.CAJERO);
        if (enMostrador) {
            usuario.verificarAccesoSucursal(solicitud.sucursalId());
        }
        sucursales.verificarActiva(solicitud.sucursalId());
        solicitud.recorrido();                                                   // 400 si el retorno es antes
        LocalDate hoy = Fechas.hoy();
        if (solicitud.fechaSolicitud().isBefore(hoy)) {                          // 400 si la fecha ya pasó
            throw ErrorAlquiler.FECHA_SOLICITUD_PASADA.excepcion(solicitud.fechaSolicitud(), hoy);
        }
        // Por requisito de calificación, la fecha la indica el usuario ("con la fecha actual como valor inicial").
        BigDecimal precioEstimado = precios.vigenteEn(solicitud.fechaSolicitud()).precioPara(solicitud.kmEstimados());
        // En un entorno real la fecha la pone el sistema:
        // BigDecimal precioEstimado = precios.vigenteEn(hoy).precioPara(solicitud.kmEstimados());
        AlquilerPrivadoEntidad alquiler = enMostrador
                ? solicitud.crearEntidad(CanalAlquiler.PRESENCIAL, null, null, precioEstimado, usuario.usuarioId())
                : solicitud.crearEntidad(CanalAlquiler.EN_LINEA, usuario.usuarioId(), usuario.correo(),
                        precioEstimado, usuario.usuarioId());
        return new AlquilerRespuesta(repositorio.saveAndFlush(alquiler), null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlquilerRespuesta> listar(FiltroAlquileres filtro) {
        return respuestas(repositorio.findAll(EspecificacionesAlquiler.filtrar(filtro), POR_SALIDA));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlquilerRespuesta> mios(UsuarioActual usuario) {
        return respuestas(repositorio.findByClienteUsuarioIdOrderByFechaSalidaDesc(usuario.usuarioId()));
    }

    @Override
    @Transactional(readOnly = true)
    public AlquilerRespuesta obtener(UUID id, UsuarioActual usuario) {
        AlquilerPrivadoEntidad alquiler = obtenerPorId(id);
        if (!usuario.esAdminSistema() && !alquiler.esDelCliente(usuario.usuarioId())
                && !esPersonalDeLaSucursal(alquiler, usuario)) {
            throw ErrorAlquiler.ALQUILER_AJENO.excepcion();
        }
        return respuesta(alquiler);
    }

    @Override
    @Transactional
    public AlquilerRespuesta cotizar(UUID id, CotizarAlquilerSolicitud solicitud, UsuarioActual usuario) {
        AlquilerPrivadoEntidad alquiler = obtenerPorId(id);
        usuario.verificarAccesoSucursal(alquiler.getSucursalId());
        VerificadorVersion.verificar(alquiler.getVersion(), solicitud.version());
        alquiler.cotizar(solicitud.precioConfirmado(), usuario.usuarioId());     // 422 si no está SOLICITADO
        alquiler = repositorio.saveAndFlush(alquiler);
        eventos.cotizacionAlquilerConfirmada(alquiler);    // Sales crea la orden de pago, Notification avisa
        return respuesta(alquiler);
    }

    @Override
    @Transactional
    public AlquilerRespuesta cancelar(UUID id, CancelarAlquilerSolicitud solicitud, UsuarioActual usuario) {
        AlquilerPrivadoEntidad alquiler = obtenerPorId(id);
        if (!alquiler.esDelCliente(usuario.usuarioId()) && !esAdminDeLaSucursal(alquiler, usuario)) {
            throw ErrorAlquiler.ALQUILER_AJENO.excepcion();
        }
        VerificadorVersion.verificar(alquiler.getVersion(), solicitud.version());
        alquiler.cancelar(solicitud.motivo());                                 // 422 si no está SOLICITADO
        return respuesta(repositorio.saveAndFlush(alquiler));
    }

    @Override
    @Transactional
    public ViajeDetalleRespuesta asignar(UUID id, AsignarAlquilerSolicitud solicitud, UsuarioActual usuario) {
        AlquilerPrivadoEntidad alquiler = obtenerPorId(id);
        usuario.verificarAccesoSucursal(alquiler.getSucursalId());
        VerificadorVersion.verificar(alquiler.getVersion(), solicitud.version());
        alquiler.verificarPagado();                                             // antes de llamar a Fleet
        AsignacionViaje asignacion = asignaciones.validarNueva(alquiler.getSucursalId(), solicitud.busId(),
                solicitud.choferId(), alquiler.getFechaSalida(), solicitud.fechaLlegadaEstimada());
        if (asignacion.capacidadBus() < alquiler.getCantidadPasajeros()) {
            throw ErrorAlquiler.CAPACIDAD_INSUFICIENTE.excepcion(asignacion.capacidadBus(),
                    alquiler.getCantidadPasajeros());
        }
        ViajeEntidad viaje = viajeRepositorio.saveAndFlush(
                ViajeEntidad.programarPrivado(alquiler, asignacion, usuario.usuarioId()));
        alquiler.asignar();
        repositorio.saveAndFlush(alquiler);
        ViajeDetalleRespuesta detalle = viajes.detalle(viaje);
        eventos.viajeProgramado(detalle.viaje());                               // tipo PRIVADO
        return detalle;
    }

    @Override
    @Transactional
    public void registrarPago(UUID alquilerId) {
        repositorio.findById(alquilerId).ifPresentOrElse(alquiler -> {
            if (alquiler.marcarPagado()) {
                repositorio.saveAndFlush(alquiler);
            } else {
                log.info("AlquilerPagado ignorado: el alquiler {} está {} (evento repetido o ya avanzó)",
                        alquilerId, alquiler.getEstado());
            }
        }, () -> log.warn("AlquilerPagado ignorado: el alquiler {} no existe", alquilerId));
    }

    private AlquilerPrivadoEntidad obtenerPorId(UUID id) {
        return repositorio.findById(id)
                .orElseThrow(() -> ErrorAlquiler.ALQUILER_NO_EXISTE.excepcion());
    }

    private static boolean esAdminDeLaSucursal(AlquilerPrivadoEntidad alquiler, UsuarioActual usuario) {
        return usuario.esAdminSistema() || (usuario.tieneRol(Roles.ADMIN_SUCURSAL)
                && alquiler.getSucursalId().equals(usuario.sucursalId()));
    }

    private static boolean esPersonalDeLaSucursal(AlquilerPrivadoEntidad alquiler, UsuarioActual usuario) {
        return usuario.tieneAlgunRol(Roles.ADMIN_SUCURSAL, Roles.CAJERO)
                && alquiler.getSucursalId().equals(usuario.sucursalId());
    }

    private AlquilerRespuesta respuesta(AlquilerPrivadoEntidad alquiler) {
        return respuestas(List.of(alquiler)).get(0);
    }

    /** Agrega a cada alquiler su viaje PRIVADO asignado (id y datos) en una sola consulta por listado. */
    private List<AlquilerRespuesta> respuestas(List<AlquilerPrivadoEntidad> alquileres) {
        if (alquileres.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = alquileres.stream().map(AlquilerPrivadoEntidad::getId).toList();
        Map<UUID, ViajeEntidad> viajePorAlquiler = new HashMap<>();
        for (ViajeEntidad viaje : viajeRepositorio.findByAlquilerIdIn(ids)) {
            viajePorAlquiler.put(viaje.getAlquilerId(), viaje);
        }
        return alquileres.stream()
                .map(alquiler -> {
                    ViajeEntidad viaje = viajePorAlquiler.get(alquiler.getId());
                    return new AlquilerRespuesta(alquiler, viaje == null ? null : viaje.getId(),
                            viaje == null ? null : viajes.resumen(viaje));
                })
                .toList();
    }
}