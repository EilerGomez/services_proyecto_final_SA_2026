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
import gt.cunoc.proyecto2.trips.utilidades.VerificadorVersion;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AlquilerServicioImpl implements AlquilerServicio {

    private static final Sort POR_SALIDA = Sort.by("fechaSalida");

    private final AlquilerPrivadoRepositorio repositorio;
    private final ConfiguracionAlquilerServicio precios;
    private final VistaSucursalServicio sucursales;
    private final AsignacionViajeServicio asignaciones;
    private final ViajeServicio viajes;
    private final ViajeRepositorio viajeRepositorio;

    public AlquilerServicioImpl(AlquilerPrivadoRepositorio repositorio, ConfiguracionAlquilerServicio precios,
                                VistaSucursalServicio sucursales, AsignacionViajeServicio asignaciones,
                                ViajeServicio viajes, ViajeRepositorio viajeRepositorio) {
        this.repositorio = repositorio;
        this.precios = precios;
        this.sucursales = sucursales;
        this.asignaciones = asignaciones;
        this.viajes = viajes;
        this.viajeRepositorio = viajeRepositorio;
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
        BigDecimal precioEstimado = precios.vigenteEn(solicitud.fechaSolicitud()).precioPara(solicitud.kmEstimados());
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
        // Paso 6: aquí se publica el evento CotizacionAlquilerConfirmada (businessDate = hoy):
        //         Sales crea la orden de pago y Notification avisa al cliente
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
        // Paso 6: aquí se publica el evento ViajeProgramado (tipo PRIVADO; businessDate = día de la salida)
        return viajes.detalle(viaje);
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

    /** Agrega a cada alquiler el id de su viaje PRIVADO (una sola consulta para todo el listado). */
    private List<AlquilerRespuesta> respuestas(List<AlquilerPrivadoEntidad> alquileres) {
        if (alquileres.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = alquileres.stream().map(AlquilerPrivadoEntidad::getId).toList();
        Map<UUID, UUID> viajePorAlquiler = viajeRepositorio.findByAlquilerIdIn(ids).stream()
                .collect(Collectors.toMap(ViajeEntidad::getAlquilerId, ViajeEntidad::getId));
        return alquileres.stream()
                .map(alquiler -> new AlquilerRespuesta(alquiler, viajePorAlquiler.get(alquiler.getId())))
                .toList();
    }
}