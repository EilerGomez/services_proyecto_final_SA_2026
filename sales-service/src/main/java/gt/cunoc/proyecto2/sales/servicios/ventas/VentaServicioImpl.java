package gt.cunoc.proyecto2.sales.servicios.ventas;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.clientes.trips.ClienteTrips;
import gt.cunoc.proyecto2.sales.dtos.trips.VendibleRespuesta;
import gt.cunoc.proyecto2.sales.dtos.ventas.CancelarVentaSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.FiltroVentas;
import gt.cunoc.proyecto2.sales.dtos.ventas.NuevaVentaEnLineaSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.NuevaVentaMostradorSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.VentaPendienteRespuesta;
import gt.cunoc.proyecto2.sales.dtos.ventas.VentaRespuesta;
import gt.cunoc.proyecto2.sales.dtos.ventas.ViajeResumenRespuesta;
import gt.cunoc.proyecto2.sales.errores.ventas.ErrorVenta;
import gt.cunoc.proyecto2.sales.errores.viajes.ErrorViaje;
import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.ClienteVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import gt.cunoc.proyecto2.sales.modelos.clientes.VistaClienteEntidad;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import gt.cunoc.proyecto2.sales.repositorios.asientos.AsientoViajeRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.clientes.VistaClienteRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.ventas.BoletoRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.ventas.EspecificacionesVenta;
import gt.cunoc.proyecto2.sales.repositorios.ventas.VentaRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.viajes.VistaViajeRepositorio;
import gt.cunoc.proyecto2.sales.servicios.viajes.VistaViajeServicio;
import gt.cunoc.proyecto2.sales.utilidades.Fechas;
import gt.cunoc.proyecto2.sales.utilidades.VerificadorVersion;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VentaServicioImpl implements VentaServicio {

    private static final Logger log = LoggerFactory.getLogger(VentaServicioImpl.class);
    private static final Sort RECIENTES_PRIMERO = Sort.by(Sort.Order.desc("fechaPago"), Sort.Order.desc("id"));

    private final VentaRepositorio repositorio;
    private final BoletoRepositorio boletos;
    private final AsientoViajeRepositorio asientos;
    private final VistaViajeServicio viajes;
    private final VistaViajeRepositorio viajeRepositorio;
    private final VistaClienteRepositorio clientes;
    private final ClienteTrips trips;
    private final int minutosReserva;

    public VentaServicioImpl(VentaRepositorio repositorio, BoletoRepositorio boletos,
                             AsientoViajeRepositorio asientos, VistaViajeServicio viajes,
                             VistaViajeRepositorio viajeRepositorio, VistaClienteRepositorio clientes,
                             ClienteTrips trips, @Value("${app.ventas.minutos-reserva:5}") int minutosReserva) {
        this.repositorio = repositorio;
        this.boletos = boletos;
        this.asientos = asientos;
        this.viajes = viajes;
        this.viajeRepositorio = viajeRepositorio;
        this.clientes = clientes;
        this.trips = trips;
        this.minutosReserva = minutosReserva;
    }

    @Override
    @Transactional
    public VentaRespuesta venderEnMostrador(NuevaVentaMostradorSolicitud solicitud, UsuarioActual usuario) {
        VistaViajeEntidad viaje = viajes.obtenerALaVenta(solicitud.viajeId());              // 404
        if (!viaje.getSucursalOrigenId().equals(usuario.sucursalId())) {
            throw ErrorVenta.OTRA_SUCURSAL.excepcion();                                      // 403
        }
        List<Integer> numeros = solicitud.asientosValidados(viaje.getCapacidad());          // 400
        verificarVendible(viaje);                                                            // R2: 422 / 503

        boolean sinNit = solicitud.clienteNit() == null
                || ClienteVenta.CONSUMIDOR_FINAL_NIT.equals(solicitud.clienteNit());
        ClienteVenta cliente = solicitud.cliente(sinNit
                ? Optional.empty()
                : clientes.findFirstByNit(solicitud.clienteNit()));          // completa nombre y correo
        VentaEntidad venta = repositorio.saveAndFlush(VentaEntidad.registrarEnMostrador(
                viaje, cliente, numeros.size(), solicitud.fechaPago(), usuario.usuarioId()));
        if (asientos.vender(viaje.getViajeId(), numeros, venta.getId()) != numeros.size()) {
            throw ErrorVenta.ASIENTO_OCUPADO.excepcion(numeros);                             // 409: rollback
        }
        List<BoletoEntidad> emitidos = boletos.saveAll(numeros.stream()
                .map(numero -> BoletoEntidad.emitir(venta, numero, viaje.getPrecioBoleto()))
                .toList());
        // Paso 6: aquí se publica el evento BoletoVendido (businessDate = fechaPago):
        //         Reporting suma el ingreso y Notification envía los boletos al correo
        return new VentaRespuesta(venta, emitidos, new ViajeResumenRespuesta(viaje));
    }

    @Override
    @Transactional
    public VentaPendienteRespuesta venderEnLinea(NuevaVentaEnLineaSolicitud solicitud, UsuarioActual usuario) {
        VistaClienteEntidad perfil = clientes.findById(usuario.usuarioId())
                .orElseThrow(() -> ErrorVenta.CLIENTE_SIN_PERFIL.excepcion());           // aún no llega ClienteRegistrado
        VistaViajeEntidad viaje = viajes.obtenerALaVenta(solicitud.viajeId());              // 404
        List<Integer> numeros = solicitud.asientosValidados(viaje.getCapacidad());          // 400
        verificarVendible(viaje);                                                            // R2: 422 / 503

        ClienteVenta cliente = new ClienteVenta(perfil.getUsuarioId(), perfil.getNit(), perfil.getNombre(),
                perfil.getCorreo());
        VentaEntidad venta = repositorio.saveAndFlush(VentaEntidad.registrarEnLinea(
                viaje, cliente, numeros.size(), solicitud.fechaPago()));
        OffsetDateTime reservadoHasta = Fechas.ahora().plusMinutes(minutosReserva);
        if (asientos.reservar(viaje.getViajeId(), numeros, venta.getId(), reservadoHasta) != numeros.size()) {
            throw ErrorVenta.ASIENTO_OCUPADO.excepcion(numeros);                             // 409: rollback
        }
        // Paso 6: aquí se publica el evento DebitoCarteraSolicitado (referenciaTipo VENTA, referenciaId ventaId,
        //         usuarioId, monto, fechaMovimiento = fechaPago; businessDate = fechaPago).
        //         Identity responde CarteraDebitada (→ CONFIRMADA y boletos) o DebitoCarteraRechazado.
        return new VentaPendienteRespuesta(venta, numeros, reservadoHasta);
    }

    @Override
    @Transactional
    public VentaRespuesta cancelar(UUID id, CancelarVentaSolicitud solicitud, UsuarioActual usuario) {
        VentaEntidad venta = obtenerPorId(id);
        usuario.verificarAccesoSucursal(venta.getSucursalId());                              // 403 SUCURSAL_AJENA
        VerificadorVersion.verificar(venta.getVersion(), solicitud.version());               // 409
        venta.cancelar(solicitud.motivo(), solicitud.fechaCancelacion());                    // 422 / 400
        VistaViajeEntidad viaje = viajeRepositorio.findById(venta.getViajeId())
                .filter(VistaViajeEntidad::estaALaVenta)
                .orElseThrow(() -> ErrorVenta.VIAJE_YA_INICIO.excepcion());                // rollback
        venta = repositorio.saveAndFlush(venta);
        List<BoletoEntidad> suyos = boletos.findByVentaIdOrderByNumeroAsientoAsc(id);
        suyos.forEach(BoletoEntidad::cancelar);
        boletos.saveAll(suyos);
        asientos.liberarDeVenta(id);
        // Paso 6: aquí se publica VentaCancelada (businessDate = fechaCancelacion: Reporting resta el ingreso,
        //         Notification avisa) y, si se pagó con CARTERA, ReembolsoCarteraSolicitado (motivo).
        return new VentaRespuesta(venta, suyos, new ViajeResumenRespuesta(viaje));
    }

    @Override
    @Transactional
    public int liberarReservasVencidas() {
        int rechazadas = repositorio.rechazarPendientesVencidas(minutosReserva);   // primero: usa los asientos
        int liberados = asientos.liberarReservasVencidas();
        if (rechazadas > 0 || liberados > 0) {
            log.info("Reservas vencidas: {} ventas rechazadas por tiempo, {} asientos liberados", rechazadas, liberados);
        }
        return rechazadas;
    }

    @Override
    @Transactional(readOnly = true)
    public VentaRespuesta obtener(UUID id, UsuarioActual usuario) {
        VentaEntidad venta = obtenerPorId(id);
        if (!puedeVer(venta, usuario)) {
            throw ErrorVenta.VENTA_AJENA.excepcion();
        }
        ViajeResumenRespuesta viaje = viajeRepositorio.findById(venta.getViajeId())
                .map(ViajeResumenRespuesta::new)
                .orElse(null);                                    // el viaje se eliminó: la venta se conserva
        return new VentaRespuesta(venta, boletos.findByVentaIdOrderByNumeroAsientoAsc(id), viaje);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaRespuesta> listar(FiltroVentas filtro) {
        List<VentaEntidad> ventas = repositorio.findAll(EspecificacionesVenta.filtrar(filtro), RECIENTES_PRIMERO);
        if (ventas.isEmpty()) {
            return List.of();
        }
        Map<UUID, List<BoletoEntidad>> boletosPorVenta = boletos
                .findByVentaIdIn(ventas.stream().map(VentaEntidad::getId).toList()).stream()
                .collect(Collectors.groupingBy(BoletoEntidad::getVentaId));
        return ventas.stream()
                .map(venta -> new VentaRespuesta(venta, ordenados(boletosPorVenta.get(venta.getId())), null))
                .toList();
    }

    private VentaEntidad obtenerPorId(UUID id) {
        return repositorio.findById(id)
                .orElseThrow(() -> ErrorVenta.VENTA_NO_EXISTE.excepcion());
    }

    /** R2: la copia local puede ir atrasada; Trips tiene la última palabra antes de vender. */
    private void verificarVendible(VistaViajeEntidad viaje) {
        VendibleRespuesta respuesta = trips.consultarVendible(viaje.getViajeId());
        if (!respuesta.vendible()) {
            throw ErrorViaje.VIAJE_NO_VENDIBLE.excepcion(respuesta.motivo());
        }
    }

    /** Dueño (con cuenta), personal (ADMIN_SUCURSAL, CAJERO) de la sucursal de la venta o ADMIN_SISTEMA. */
    private static boolean puedeVer(VentaEntidad venta, UsuarioActual usuario) {
        boolean personalDeLaSucursal = usuario.tieneAlgunRol(Roles.ADMIN_SUCURSAL, Roles.CAJERO)
                && venta.getSucursalId().equals(usuario.sucursalId());
        return usuario.esAdminSistema() || personalDeLaSucursal || venta.esDelCliente(usuario.usuarioId());
    }

    private static List<BoletoEntidad> ordenados(List<BoletoEntidad> lista) {
        if (lista == null) {
            return List.of();
        }
        return lista.stream()
                .sorted(Comparator.comparingInt(BoletoEntidad::getNumeroAsiento))
                .toList();
    }
}