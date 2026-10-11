package gt.cunoc.proyecto2.sales.servicios.alquileres;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.dtos.alquileres.FiltroOrdenes;
import gt.cunoc.proyecto2.sales.dtos.alquileres.OrdenAlquilerRespuesta;
import gt.cunoc.proyecto2.sales.dtos.alquileres.PagoOrdenSolicitud;
import gt.cunoc.proyecto2.sales.errores.alquileres.ErrorOrdenAlquiler;
import gt.cunoc.proyecto2.sales.eventos.PublicadorEventosSales;
import gt.cunoc.proyecto2.sales.eventos.recibidos.trips.CotizacionAlquilerConfirmada;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.ClienteVenta;
import gt.cunoc.proyecto2.sales.repositorios.alquileres.EspecificacionesOrden;
import gt.cunoc.proyecto2.sales.repositorios.alquileres.OrdenAlquilerRepositorio;
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
    private final PublicadorEventosSales eventos;

    public OrdenAlquilerServicioImpl(OrdenAlquilerRepositorio repositorio, PublicadorEventosSales eventos) {
        this.repositorio = repositorio;
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
        // Saga: Identity responde CarteraDebitada (→ PAGADA y AlquilerPagado) o DebitoCarteraRechazado
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
        ClienteVenta cliente = new ClienteVenta(cotizacion.clienteUsuarioId(), cotizacion.clienteNit(),
                cotizacion.clienteNombre(), cotizacion.clienteCorreo());
        repositorio.save(OrdenAlquilerEntidad.registrar(cotizacion.alquilerId(), cotizacion.sucursalId(), cliente,
                cotizacion.origen(), cotizacion.destino(), cotizacion.precioConfirmado()));
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
