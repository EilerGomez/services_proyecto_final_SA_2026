package gt.cunoc.proyecto2.trips.servicios.alquileres;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.trips.dtos.alquileres.AlquilerRespuesta;
import gt.cunoc.proyecto2.trips.dtos.alquileres.AsignarAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.CancelarAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.CotizarAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.FiltroAlquileres;
import gt.cunoc.proyecto2.trips.dtos.alquileres.NuevoAlquilerSolicitud;
import gt.cunoc.proyecto2.trips.dtos.alquileres.PoliticaCancelacionRespuesta;
import gt.cunoc.proyecto2.trips.dtos.viajes.ViajeDetalleRespuesta;
import gt.cunoc.proyecto2.trips.errores.alquileres.ErrorAlquiler;
import gt.cunoc.proyecto2.trips.eventos.PublicadorEventosTrips;
import gt.cunoc.proyecto2.trips.eventos.publicados.alquileres.AlquilerCancelado;
import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import gt.cunoc.proyecto2.trips.modelos.alquileres.CanalAlquiler;
import gt.cunoc.proyecto2.trips.modelos.alquileres.ClienteAlquiler;
import gt.cunoc.proyecto2.trips.modelos.alquileres.EstadoAlquiler;
import gt.cunoc.proyecto2.trips.modelos.clientes.VistaClienteEntidad;
import gt.cunoc.proyecto2.trips.modelos.viajes.AsignacionViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.repositorios.alquileres.AlquilerPrivadoRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.alquileres.EspecificacionesAlquiler;
import gt.cunoc.proyecto2.trips.repositorios.clientes.VistaClienteRepositorio;
import gt.cunoc.proyecto2.trips.repositorios.viajes.ViajeRepositorio;
import gt.cunoc.proyecto2.trips.servicios.sucursales.VistaSucursalServicio;
import gt.cunoc.proyecto2.trips.servicios.tarifas.PoliticaCancelacionServicio;
import gt.cunoc.proyecto2.trips.servicios.viajes.AsignacionViajeServicio;
import gt.cunoc.proyecto2.trips.servicios.viajes.ViajeServicio;
import gt.cunoc.proyecto2.trips.utilidades.Fechas;
import gt.cunoc.proyecto2.trips.utilidades.VerificadorVersion;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
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
    /**
     * Listados de gestión y los del cliente: primero lo último pedido o modificado. La fecha de
     * salida desempata, para que dos alquileres tocados en el mismo instante no cambien de orden.
     */
    private static final Sort RECIENTES = Sort.by(Sort.Direction.DESC, "fechaActualizacion", "fechaSalida");

    private final AlquilerPrivadoRepositorio repositorio;
    private final ConfiguracionAlquilerServicio precios;
    private final VistaSucursalServicio sucursales;
    private final AsignacionViajeServicio asignaciones;
    private final ViajeServicio viajes;
    private final ViajeRepositorio viajeRepositorio;
    private final VistaClienteRepositorio clientes;
    private final PoliticaCancelacionServicio politicas;
    private final PublicadorEventosTrips eventos;

    public AlquilerServicioImpl(AlquilerPrivadoRepositorio repositorio, ConfiguracionAlquilerServicio precios,
                                VistaSucursalServicio sucursales, AsignacionViajeServicio asignaciones,
                                ViajeServicio viajes, ViajeRepositorio viajeRepositorio,
                                VistaClienteRepositorio clientes, PoliticaCancelacionServicio politicas,
                                PublicadorEventosTrips eventos) {
        this.repositorio = repositorio;
        this.precios = precios;
        this.sucursales = sucursales;
        this.asignaciones = asignaciones;
        this.viajes = viajes;
        this.viajeRepositorio = viajeRepositorio;
        this.clientes = clientes;
        this.politicas = politicas;
        this.eventos = eventos;
    }

    @Override
    @Transactional
    public AlquilerRespuesta solicitar(NuevoAlquilerSolicitud solicitud, UsuarioActual usuario) {
        // La ventanilla (cajero y administrador de sucursal) pide a nombre de quien llega al mostrador,
        // que puede ser cualquiera, incluso el propio empleado. Puede pedirlo en cualquier sucursal:
        // el alquiler no se atiende donde trabaja, sino donde sale el bus.
        boolean enMostrador = usuario.tieneAlgunRol(Roles.CAJERO, Roles.ADMIN_SUCURSAL);
        sucursales.verificarActiva(solicitud.sucursalId());
        solicitud.recorrido();                                                   // 400 si el retorno es antes
        if (solicitud.fechaSalida().isBefore(Fechas.ahora())) {                  // 400 si ya pasó la hora
            throw ErrorAlquiler.SALIDA_EN_EL_PASADO.excepcion(Fechas.enGuatemala(solicitud.fechaSalida()));
        }
        LocalDate hoy = Fechas.hoy();
        if (solicitud.fechaSolicitud().isBefore(hoy)) {                          // 400 si la fecha ya pasó
            throw ErrorAlquiler.FECHA_SOLICITUD_PASADA.excepcion(solicitud.fechaSolicitud(), hoy);
        }
        // Por requisito de calificación, la fecha la indica el usuario ("con la fecha actual como valor inicial").
        BigDecimal precioEstimado = precios.vigenteEn(solicitud.fechaSolicitud()).precioPara(solicitud.kmEstimados());
        // En un entorno real la fecha la pone el sistema:
        // BigDecimal precioEstimado = precios.vigenteEn(hoy).precioPara(solicitud.kmEstimados());
        AlquilerPrivadoEntidad alquiler = enMostrador
                ? solicitud.crearEntidad(CanalAlquiler.PRESENCIAL, cuentaDelNit(solicitud.clienteNit()), null,
                        precioEstimado, usuario.usuarioId())
                : solicitud.crearEntidad(CanalAlquiler.EN_LINEA, usuario.usuarioId(), usuario.correo(),
                        precioEstimado, usuario.usuarioId());
        return new AlquilerRespuesta(repositorio.saveAndFlush(alquiler), null);
    }

    /**
     * Cuenta a la que pertenece el NIT que tecleó la ventanilla, o null si no es de nadie. Con ella
     * el alquiler le aparece al cliente en "Mis alquileres" y lo puede pagar con su billetera, aunque
     * lo haya pedido en el mostrador; sin ella solo se puede cobrar en efectivo.
     */
    private UUID cuentaDelNit(String nit) {
        if (nit == null || nit.isBlank() || ClienteAlquiler.CONSUMIDOR_FINAL.equalsIgnoreCase(nit)) {
            return null;
        }
        return clientes.findFirstByNitIgnoreCase(nit)
                .map(VistaClienteEntidad::getUsuarioId)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AlquilerRespuesta> listar(FiltroAlquileres filtro) {
        return respuestas(repositorio.findAll(EspecificacionesAlquiler.filtrar(filtro), RECIENTES));
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

    /**
     * T21. Se cancela desde SOLICITADO, COTIZADO, PAGADO o ASIGNADO, y lo puede hacer tanto el cliente
     * dueño como el administrador de la sucursal: desde PAGADO el cliente cancela bajo su propia
     * responsabilidad, aceptando la penalización de la tarifa.
     *
     * <p>Orden de los pasos, que importa: primero la tarifa decide si la cancelación se permite (si no,
     * no se toca nada), después se borra el viaje si había, y de último se cancela el alquiler y sale
     * AlquilerCancelado. Todo en una transacción: si algo falla, no se borra el viaje ni se mueve dinero.</p>
     */
    @Override
    @Transactional
    public AlquilerRespuesta cancelar(UUID id, CancelarAlquilerSolicitud solicitud, UsuarioActual usuario) {
        AlquilerPrivadoEntidad alquiler = obtenerPorId(id);
        if (!alquiler.esDelCliente(usuario.usuarioId()) && !esAdminDeLaSucursal(alquiler, usuario)) {
            throw ErrorAlquiler.ALQUILER_AJENO.excepcion();
        }
        VerificadorVersion.verificar(alquiler.getVersion(), solicitud.version());
        if (!EstadoAlquiler.CANCELABLES.contains(alquiler.getEstado())) {
            throw ErrorAlquiler.ESTADO_NO_CANCELABLE.excepcion(alquiler.getEstado());     // 422
        }
        verificarPuedeCancelarlo(alquiler, usuario);                                      // 403
        OffsetDateTime cuando = cuandoSeCancela(alquiler, solicitud.fechaHoraCancelacion());
        PoliticaCancelacionRespuesta politica = politicas.para(alquiler, cuando);
        if (!politica.permiteCancelacion()) {
            // El mensaje es la justificación de la tarifa: el usuario ve el porqué, no un código
            throw ErrorAlquiler.CANCELACION_NO_PERMITIDA.excepcion(politica.justificacion());
        }
        EstadoAlquiler estadoAnterior = alquiler.getEstado();
        // Si tenía bus y piloto, el viaje desaparece: la unidad queda libre para otro alquiler
        UUID viajeBorrado = viajes.eliminarPorCancelacionDeAlquiler(id, solicitud.motivo());
        alquiler.cancelar(solicitud.motivo(), cuando);
        AlquilerPrivadoEntidad cancelado = repositorio.saveAndFlush(alquiler);
        // Sales cancela la orden y, si ya había pago, devuelve el porcentaje que indique su tabla
        eventos.alquilerCancelado(cancelado, AlquilerCancelado.desde(cancelado, estadoAnterior, cuando,
                politica.horasAntesDeSalida(), viajeBorrado));
        log.info("Alquiler {} cancelado desde {} con {} horas de anticipación", id, estadoAnterior,
                politica.horasAntesDeSalida());
        return respuesta(cancelado);
    }

    @Override
    @Transactional(readOnly = true)
    public PoliticaCancelacionRespuesta politicaDeCancelacion(UUID id, OffsetDateTime cuando,
                                                              UsuarioActual usuario) {
        AlquilerPrivadoEntidad alquiler = obtenerPorId(id);
        if (!usuario.esAdminSistema() && !alquiler.esDelCliente(usuario.usuarioId())
                && !esPersonalDeLaSucursal(alquiler, usuario)) {
            throw ErrorAlquiler.ALQUILER_AJENO.excepcion();
        }
        return politicas.para(alquiler, cuando == null ? Fechas.ahora() : cuando);
    }

    /**
     * El momento que indicó quien cancela, o ahora si no lo mandó. No puede ser anterior a la fecha de
     * solicitud del alquiler: un tramo calculado hacia atrás regalaría un reembolso que no corresponde.
     */
    private static OffsetDateTime cuandoSeCancela(AlquilerPrivadoEntidad alquiler, OffsetDateTime indicada) {
        if (indicada == null) {
            return Fechas.ahora();
        }
        if (Fechas.diaEnGuatemala(indicada).isBefore(alquiler.getFechaSolicitud())) {
            throw ErrorAlquiler.CANCELACION_EN_EL_PASADO.excepcion(Fechas.diaEnGuatemala(indicada));
        }
        return indicada;
    }

    @Override
    @Transactional
    public ViajeDetalleRespuesta asignar(UUID id, AsignarAlquilerSolicitud solicitud, UsuarioActual usuario) {
        AlquilerPrivadoEntidad alquiler = obtenerPorId(id);
        usuario.verificarAccesoSucursal(alquiler.getSucursalId());
        VerificadorVersion.verificar(alquiler.getVersion(), solicitud.version());
        alquiler.verificarPagado();                                             // antes de llamar a Fleet
        verificarLlegadaCubreElRetorno(alquiler, solicitud.fechaLlegadaEstimada());
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
        eventos.alquilerAsignado(alquiler, detalle.viaje());                    // avisa al cliente (bus y piloto)
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

    /**
     * T22. El viaje privado tiene que cubrir el retorno que pidió el cliente: si el alquiler es de
     * ida y vuelta, el bus no puede estar de regreso antes de la hora a la que lo van a ocupar.
     */
    private static void verificarLlegadaCubreElRetorno(AlquilerPrivadoEntidad alquiler,
                                                       OffsetDateTime fechaLlegadaEstimada) {
        OffsetDateTime retorno = alquiler.getFechaRetorno();
        if (retorno != null && fechaLlegadaEstimada.isBefore(retorno)) {
            throw ErrorAlquiler.LLEGADA_ANTES_DEL_RETORNO.excepcion(Fechas.enGuatemala(retorno));
        }
    }

    /**
     * T21. Mientras el alquiler no esté pagado (SOLICITADO o COTIZADO), solo lo cancela la sucursal
     * que lo atiende: el cliente todavía está negociando el precio y una cancelación suya ahí deja
     * la cotización a medias. Ya pagado o asignado sí puede cancelarlo él, con su reembolso.
     */
    private static void verificarPuedeCancelarlo(AlquilerPrivadoEntidad alquiler, UsuarioActual usuario) {
        boolean sinPagar = alquiler.getEstado() == EstadoAlquiler.SOLICITADO
                || alquiler.getEstado() == EstadoAlquiler.COTIZADO;
        if (sinPagar && !esAdminDeLaSucursal(alquiler, usuario)) {
            throw ErrorAlquiler.SOLO_LA_SUCURSAL_CANCELA.excepcion(alquiler.getEstado());
        }
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