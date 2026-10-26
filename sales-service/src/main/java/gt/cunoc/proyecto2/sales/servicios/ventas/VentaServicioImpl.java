package gt.cunoc.proyecto2.sales.servicios.ventas;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.clientes.trips.ClienteTrips;
import gt.cunoc.proyecto2.sales.dtos.trips.VendibleRespuesta;
import gt.cunoc.proyecto2.sales.dtos.ventas.CancelarVentaSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.FiltroVentas;
import gt.cunoc.proyecto2.sales.dtos.ventas.NuevaVentaEnLineaSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.NuevaVentaMostradorSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.ReembolsoVentaSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.VentaPendienteRespuesta;
import gt.cunoc.proyecto2.sales.dtos.ventas.VentaRespuesta;
import gt.cunoc.proyecto2.sales.dtos.ventas.ViajeResumenRespuesta;
import gt.cunoc.proyecto2.sales.errores.ventas.ErrorVenta;
import gt.cunoc.proyecto2.sales.errores.viajes.ErrorViaje;
import gt.cunoc.proyecto2.sales.eventos.MotivoSaga;
import gt.cunoc.proyecto2.sales.eventos.PublicadorEventosSales;
import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.ClienteVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.EstadoVenta;
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
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VentaServicioImpl implements VentaServicio {

    private static final Logger log = LoggerFactory.getLogger(VentaServicioImpl.class);
    /** motivo_cancelacion (V11). Lo que no quepa se recorta. */
    private static final int LARGO_MOTIVO = 500;
    private static final String MOTIVO_VIAJE_ELIMINADO = "Viaje eliminado";
    private static final String SEPARADOR = " · ";
    private static final DateTimeFormatter SALIDA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private final VentaRepositorio repositorio;
    private final BoletoRepositorio boletos;
    private final AsientoViajeRepositorio asientos;
    private final VistaViajeServicio viajes;
    private final VistaViajeRepositorio viajeRepositorio;
    private final VistaClienteRepositorio clientes;
    private final ClienteTrips trips;
    private final PublicadorEventosSales eventos;
    private final int minutosReserva;

    public VentaServicioImpl(VentaRepositorio repositorio, BoletoRepositorio boletos,
                             AsientoViajeRepositorio asientos, VistaViajeServicio viajes,
                             VistaViajeRepositorio viajeRepositorio, VistaClienteRepositorio clientes,
                             ClienteTrips trips, PublicadorEventosSales eventos,
                             @Value("${app.ventas.minutos-reserva:5}") int minutosReserva) {
        this.repositorio = repositorio;
        this.boletos = boletos;
        this.asientos = asientos;
        this.viajes = viajes;
        this.viajeRepositorio = viajeRepositorio;
        this.clientes = clientes;
        this.trips = trips;
        this.eventos = eventos;
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
        eventos.boletoVendido(venta, emitidos, viaje);        // Reporting suma el ingreso; Notification envía
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
        // Saga: Wallet responde CarteraDebitada (→ CONFIRMADA y boletos) o DebitoCarteraRechazado
        eventos.debitoVenta(venta);
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
        List<BoletoEntidad> suyos = cerrarCancelacion(venta, viaje, MotivoSaga.VENTA_CANCELADA);
        return new VentaRespuesta(venta, suyos, new ViajeResumenRespuesta(viaje));
    }

    @Override
    @Transactional
    public VentaRespuesta reembolsar(UUID id, ReembolsoVentaSolicitud solicitud, UsuarioActual usuario) {
        VentaEntidad venta = obtenerPorId(id);
        usuario.verificarAccesoSucursal(venta.getSucursalId());                              // 403 SUCURSAL_AJENA
        VerificadorVersion.verificar(venta.getVersion(), solicitud.version());               // 409
        if (venta.estaReembolsada()) {
            throw ErrorVenta.VENTA_YA_REEMBOLSADA.excepcion();                               // 422
        }
        if (!venta.puedeMarcarseReembolsada()) {
            throw ErrorVenta.ESTADO_INVALIDO.excepcion(venta.getEstado(), "CANCELADA o RECHAZADA");
        }
        if (venta.pagadaConCartera()) {
            // En línea: el dinero lo devuelve Wallet. Esto solo vuelve a pedirlo (Wallet no repite el
            // reembolso de la misma venta), y el estado cambia cuando llegue CarteraReembolsada.
            eventos.reembolsoVenta(venta, MotivoSaga.VENTA_CANCELADA, venta.fechaNoAnteriorAlPago(Fechas.hoy()));
            log.info("Venta {} pagada con cartera: se volvió a pedir el reembolso a Wallet", id);
            return conViaje(venta);
        }
        venta.reembolsarEnEfectivo(Fechas.hoy(), usuario.usuarioId());
        VentaEntidad reembolsada = repositorio.saveAndFlush(venta);
        List<BoletoEntidad> suyos = boletos.findByVentaIdOrderByNumeroAsientoAsc(id);
        VistaViajeEntidad viaje = viajeRepositorio.findById(reembolsada.getViajeId()).orElse(null);
        eventos.ventaReembolsada(reembolsada, suyos, viaje);          // Notification avisa al cliente
        log.info("Venta {} reembolsada en efectivo por {}", id, usuario.usuarioId());
        return new VentaRespuesta(reembolsada, suyos,
                viaje == null ? null : new ViajeResumenRespuesta(viaje));
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaRespuesta> buscarPorCodigo(String codigo, UsuarioActual usuario) {
        String normalizado = VentaEntidad.normalizarCodigo(codigo);
        if (normalizado == null) {
            throw ErrorVenta.CODIGO_INVALIDO.excepcion();                                    // 400
        }
        List<VentaEntidad> encontradas = repositorio.buscarPorCodigo(normalizado).stream()
                .filter(venta -> puedeVer(venta, usuario))
                .toList();
        if (encontradas.isEmpty()) {
            throw ErrorVenta.VENTA_NO_EXISTE.excepcion();                                    // 404
        }
        return encontradas.stream().map(this::conViaje).toList();
    }

    @Override
    @Transactional
    public void compensarViajeEliminado(UUID viajeId, String motivo) {
        VistaViajeEntidad viaje = viajeRepositorio.findById(viajeId).orElse(null);
        String motivoCancelacion = motivoViajeEliminado(viaje, motivo);
        LocalDate hoy = Fechas.hoy();
        List<VentaEntidad> afectadas = repositorio.findByViajeIdAndEstadoIn(viajeId,
                List.of(EstadoVenta.PENDIENTE_PAGO, EstadoVenta.CONFIRMADA));
        for (VentaEntidad venta : afectadas) {
            if (venta.estaPendienteDePago()) {
                // Si después llega su CarteraDebitada, la saga ve RECHAZADA y pide el reembolso
                venta.rechazar(MotivoSaga.VIAJE_ELIMINADO.name());
                repositorio.saveAndFlush(venta);
                asientos.liberarDeVenta(venta.getId());
            } else {
                venta.cancelarPorViajeEliminado(motivoCancelacion, hoy);
                cerrarCancelacion(repositorio.saveAndFlush(venta), viaje, MotivoSaga.VIAJE_ELIMINADO);
            }
        }
        if (!afectadas.isEmpty()) {
            log.info("Viaje {} eliminado: {} ventas rechazadas o canceladas", viajeId, afectadas.size());
        }
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
        return conViaje(venta);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaRespuesta> listar(FiltroVentas filtro) {
        List<VentaEntidad> ventas = repositorio.findAll(EspecificacionesVenta.filtrar(filtro),
                VentaRepositorio.RECIENTES_PRIMERO);
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

    /**
     * Después de guardar la venta CANCELADA: boletos CANCELADO, asientos libres, VentaCancelada
     * (businessDate = fecha de cancelación) y, si se pagó con cartera, el reembolso con esa misma fecha.
     */
    private List<BoletoEntidad> cerrarCancelacion(VentaEntidad venta, VistaViajeEntidad viaje,
                                                  MotivoSaga motivoReembolso) {
        List<BoletoEntidad> suyos = boletos.findByVentaIdOrderByNumeroAsientoAsc(venta.getId());
        suyos.forEach(BoletoEntidad::cancelar);
        boletos.saveAll(suyos);
        asientos.liberarDeVenta(venta.getId());
        eventos.ventaCancelada(venta, suyos, viaje);
        if (venta.pagadaConCartera()) {
            eventos.reembolsoVenta(venta, motivoReembolso, venta.getFechaCancelacion());
        }
        return suyos;
    }

    /** Venta con sus boletos y el resumen del viaje, o sin viaje si ya se eliminó (la venta se conserva). */
    private VentaRespuesta conViaje(VentaEntidad venta) {
        ViajeResumenRespuesta viaje = viajeRepositorio.findById(venta.getViajeId())
                .map(ViajeResumenRespuesta::new)
                .orElse(null);
        return new VentaRespuesta(venta, boletos.findByVentaIdOrderByNumeroAsientoAsc(venta.getId()), viaje);
    }

    /**
     * Motivo de cancelación cuando Trips elimina el viaje. La fila del viaje se borra de vista_viajes
     * en la misma transacción, así que el origen, el destino, la salida y la placa se copian aquí: en la
     * pantalla de reembolsos es lo único que dice de qué viaje era el boleto que trae el cliente.
     * Ejemplo: "Viaje eliminado · Xela → Guatemala · 25/10/2026 08:00 · Bus P123ABC · bus en taller".
     */
    private static String motivoViajeEliminado(VistaViajeEntidad viaje, String motivo) {
        StringBuilder texto = new StringBuilder(MOTIVO_VIAJE_ELIMINADO);
        if (viaje != null) {
            texto.append(SEPARADOR).append(viaje.getOrigenNombre()).append(" → ").append(viaje.getDestinoNombre())
                    .append(SEPARADOR).append(SALIDA.format(Fechas.enGuatemala(viaje.getFechaSalida())));
            if (viaje.getBusPlaca() != null && !viaje.getBusPlaca().isBlank()) {
                texto.append(SEPARADOR).append("Bus ").append(viaje.getBusPlaca());
            }
        }
        if (motivo != null && !motivo.isBlank()) {
            texto.append(SEPARADOR).append(motivo.strip());
        }
        return texto.length() <= LARGO_MOTIVO ? texto.toString() : texto.substring(0, LARGO_MOTIVO);
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
