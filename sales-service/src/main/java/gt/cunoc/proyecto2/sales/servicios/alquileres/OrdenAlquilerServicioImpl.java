package gt.cunoc.proyecto2.sales.servicios.alquileres;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.dtos.alquileres.FiltroOrdenes;
import gt.cunoc.proyecto2.sales.dtos.alquileres.OrdenAlquilerRespuesta;
import gt.cunoc.proyecto2.sales.dtos.alquileres.PagoOrdenSolicitud;
import gt.cunoc.proyecto2.sales.dtos.alquileres.ReembolsoOrdenSolicitud;
import gt.cunoc.proyecto2.sales.dtos.tarifas.ReglaReembolsoRespuesta;
import gt.cunoc.proyecto2.sales.errores.alquileres.ErrorOrdenAlquiler;
import gt.cunoc.proyecto2.sales.eventos.MotivoSaga;
import gt.cunoc.proyecto2.sales.eventos.PublicadorEventosSales;
import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.AlquilerCancelado;
import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.CotizacionAlquilerConfirmada;
import gt.cunoc.proyecto2.sales.modelos.alquileres.EstadoOrdenAlquiler;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import gt.cunoc.proyecto2.sales.modelos.clientes.VistaClienteEntidad;
import gt.cunoc.proyecto2.sales.modelos.tarifas.EstadoAlquilerTarifa;
import gt.cunoc.proyecto2.sales.modelos.ventas.ClienteVenta;
import gt.cunoc.proyecto2.sales.repositorios.alquileres.EspecificacionesOrden;
import gt.cunoc.proyecto2.sales.repositorios.alquileres.OrdenAlquilerRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.clientes.VistaClienteRepositorio;
import gt.cunoc.proyecto2.sales.servicios.tarifas.TarifaReembolsoServicio;
import gt.cunoc.proyecto2.sales.utilidades.Fechas;
import gt.cunoc.proyecto2.sales.utilidades.VerificadorVersion;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrdenAlquilerServicioImpl implements OrdenAlquilerServicio {

    private static final Logger log = LoggerFactory.getLogger(OrdenAlquilerServicioImpl.class);
    private static final Sort RECIENTES_PRIMERO = Sort.by(Sort.Order.desc("fechaCreacion"));

    private final OrdenAlquilerRepositorio repositorio;
    private final VistaClienteRepositorio clientes;
    private final TarifaReembolsoServicio tarifas;
    private final PublicadorEventosSales eventos;

    public OrdenAlquilerServicioImpl(OrdenAlquilerRepositorio repositorio, VistaClienteRepositorio clientes,
                                     TarifaReembolsoServicio tarifas, PublicadorEventosSales eventos) {
        this.repositorio = repositorio;
        this.clientes = clientes;
        this.tarifas = tarifas;
        this.eventos = eventos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenAlquilerRespuesta> listar(FiltroOrdenes filtro) {
        return repositorio.findAll(EspecificacionesOrden.filtrar(filtro), RECIENTES_PRIMERO).stream()
                .map(OrdenAlquilerRespuesta::new)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrdenAlquilerRespuesta> mias(UsuarioActual usuario) {
        return repositorio.findByClienteUsuarioIdOrderByFechaCreacionDesc(usuario.usuarioId()).stream()
                .map(OrdenAlquilerRespuesta::new)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public OrdenAlquilerRespuesta obtener(UUID id, UsuarioActual usuario) {
        OrdenAlquilerEntidad orden = obtenerPorId(id);
        if (!orden.esDelCliente(usuario.usuarioId()) && !esPersonalDeLaSucursal(orden, usuario)) {
            throw ErrorOrdenAlquiler.ORDEN_AJENA.excepcion();
        }
        return new OrdenAlquilerRespuesta(orden);
    }

    @Override
    @Transactional
    public OrdenAlquilerRespuesta pagarConCartera(UUID id, PagoOrdenSolicitud solicitud, UsuarioActual usuario) {
        OrdenAlquilerEntidad orden = obtenerPorId(id);
        if (!orden.esDelCliente(usuario.usuarioId())) {
            throw ErrorOrdenAlquiler.ORDEN_AJENA.excepcion();
        }
        orden.solicitarPagoConCartera(solicitud.fechaPago());                    
        orden = repositorio.saveAndFlush(orden);
        // Saga: Wallet responde CarteraDebitada (→ PAGADA y AlquilerPagado) o DebitoCarteraRechazado
        eventos.debitoOrden(orden);
        return new OrdenAlquilerRespuesta(orden);
    }

    @Override
    @Transactional
    public OrdenAlquilerRespuesta pagarEnEfectivo(UUID id, PagoOrdenSolicitud solicitud, UsuarioActual usuario) {
        OrdenAlquilerEntidad orden = obtenerPorId(id);
        usuario.verificarAccesoSucursal(orden.getSucursalId());                    // 403 SUCURSAL_AJENA
        orden.pagarEnEfectivo(solicitud.fechaPago());                              // 422
        orden = repositorio.saveAndFlush(orden);
        eventos.alquilerPagado(orden);        // Trips → PAGADO, Reporting suma el ingreso, Notification avisa
        return new OrdenAlquilerRespuesta(orden);
    }

    @Override
    @Transactional
    public void registrarOrden(CotizacionAlquilerConfirmada cotizacion) {
        if (repositorio.existsByAlquilerId(cotizacion.alquilerId())) {
            log.info("El alquiler {} ya tiene orden de pago: se ignora", cotizacion.alquilerId());
            return;
        }
        ClienteVenta cliente = new ClienteVenta(cotizacion.clienteUsuarioId(), nitDelCliente(cotizacion),
                cotizacion.clienteNombre(), cotizacion.clienteCorreo());
        repositorio.save(OrdenAlquilerEntidad.registrar(cotizacion.alquilerId(), cotizacion.sucursalId(), cliente,
                cotizacion.origen(), cotizacion.destino(), cotizacion.recorrido(), cotizacion.fechaSalida(),
                cotizacion.fechaRetorno(), cotizacion.precioConfirmado()));
    }

    @Override
    @Transactional
    public void registrarCancelacion(AlquilerCancelado aviso, LocalDate fechaNegocio) {
        OrdenAlquilerEntidad orden = repositorio.findByAlquilerId(aviso.alquilerId()).orElse(null);
        if (orden == null) {
            // Se canceló antes de cotizar: nunca existió orden de pago y no hay nada que devolver
            log.info("AlquilerCancelado del alquiler {} sin orden de pago: no se hace nada", aviso.alquilerId());
            return;
        }
        if (orden.yaCerrada()) {
            log.info("AlquilerCancelado repetido para la orden {} ({}): no se hace nada", orden.getId(),
                    orden.getEstado());
            return;
        }
        LocalDate fecha = orden.fechaNoAnteriorAlPago(fechaNegocio);
        boolean habiaDinero = orden.getEstado() == EstadoOrdenAlquiler.PAGADA;
        // La tarifa solo se resuelve si hay dinero: sin pago no hay porcentaje que aplicar
        ReglaReembolsoRespuesta regla = habiaDinero
                ? tarifas.aplicable(EstadoAlquilerTarifa.deNombre(aviso.estadoAnterior()), aviso.horas())
                : null;
        orden.cancelar(aviso.motivo(), fecha, (int) aviso.horas(),
                regla == null ? null : regla.porcentajeReembolso(),
                regla == null ? null : regla.justificacion());
        OrdenAlquilerEntidad cancelada = repositorio.saveAndFlush(orden);

        if (!cancelada.tieneReembolsoPendiente()) {
            log.info("Orden {} cancelada sin dinero por devolver ({})", cancelada.getId(),
                    cancelada.getMontoReembolsado());
            return;
        }
        if (cancelada.seCobroConCartera()) {
            // Saga: Wallet acredita el porcentaje y responde CarteraReembolsada (→ REEMBOLSADA)
            eventos.reembolsoOrden(cancelada, MotivoSaga.ALQUILER_CANCELADO, fecha);
            log.info("Orden {} cancelada: se pide a Wallet devolver {} ({}%)", cancelada.getId(),
                    cancelada.getMontoReembolsado(), cancelada.getPorcentajeReembolso());
            return;
        }
        log.info("Orden {} cancelada: quedan {} por devolver en ventanilla ({}%)", cancelada.getId(),
                cancelada.getMontoReembolsado(), cancelada.getPorcentajeReembolso());
    }

    @Override
    @Transactional
    public OrdenAlquilerRespuesta reembolsar(UUID id, ReembolsoOrdenSolicitud solicitud, UsuarioActual usuario) {
        OrdenAlquilerEntidad orden = obtenerPorId(id);
        usuario.verificarAccesoSucursal(orden.getSucursalId());                    // 403 SUCURSAL_AJENA
        VerificadorVersion.verificar(orden.getVersion(), solicitud.version());     // 409
        if (orden.estaReembolsada() || orden.getFechaReembolso() != null) {
            throw ErrorOrdenAlquiler.ORDEN_YA_REEMBOLSADA.excepcion();             // 422
        }
        if (!orden.tieneReembolsoPendiente()) {
            throw ErrorOrdenAlquiler.ESTADO_INVALIDO_REEMBOLSO.excepcion(orden.getEstado());
        }
        if (orden.seCobroConCartera()) {
            // El dinero lo acredita Wallet. Esto solo vuelve a pedirlo (Wallet no repite el reembolso
            // de la misma orden), y el estado cambia cuando llegue CarteraReembolsada.
            eventos.reembolsoOrden(orden, MotivoSaga.ALQUILER_CANCELADO,
                    orden.fechaNoAnteriorAlPago(Fechas.hoy()));
            log.info("Orden {} pagada con cartera: se volvió a pedir el reembolso a Wallet", id);
            return new OrdenAlquilerRespuesta(orden);
        }
        orden.marcarReembolsada(Fechas.hoy(), usuario.usuarioId());
        OrdenAlquilerEntidad reembolsada = repositorio.saveAndFlush(orden);
        log.info("Orden {} reembolsada en efectivo por {}", id, usuario.usuarioId());
        return new OrdenAlquilerRespuesta(reembolsada);
    }

    /**
     * El alquiler EN_LINEA no trae NIT (lo pidió el propio cliente): se completa con el de su perfil para que
     * la orden quede enlazada a su historial de compras. Sin perfil (o PRESENCIAL sin NIT) cae a "CF".
     */
    private String nitDelCliente(CotizacionAlquilerConfirmada cotizacion) {
        if (cotizacion.clienteNit() != null && !cotizacion.clienteNit().isBlank()) {
            return cotizacion.clienteNit();
        }
        UUID usuarioId = cotizacion.clienteUsuarioId();
        return usuarioId == null ? null
                : clientes.findById(usuarioId).map(VistaClienteEntidad::getNit).orElse(null);
    }

    private OrdenAlquilerEntidad obtenerPorId(UUID id) {
        return repositorio.findById(id)
                .orElseThrow(() -> ErrorOrdenAlquiler.ORDEN_NO_EXISTE.excepcion());
    }

    /*ADMIN_SISTEMA, o ADMIN_SUCURSAL / CAJERO de la sucursal del alquiler. */
    private static boolean esPersonalDeLaSucursal(OrdenAlquilerEntidad orden, UsuarioActual usuario) {
        return usuario.esAdminSistema() || (usuario.tieneAlgunRol(Roles.ADMIN_SUCURSAL, Roles.CAJERO)
                && orden.getSucursalId().equals(usuario.sucursalId()));
    }
}
