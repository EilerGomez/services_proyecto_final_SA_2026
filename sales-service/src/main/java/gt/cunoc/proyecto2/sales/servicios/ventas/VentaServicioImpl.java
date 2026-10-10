package gt.cunoc.proyecto2.sales.servicios.ventas;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.clientes.trips.ClienteTrips;
import gt.cunoc.proyecto2.sales.dtos.trips.VendibleRespuesta;
import gt.cunoc.proyecto2.sales.dtos.ventas.FiltroVentas;
import gt.cunoc.proyecto2.sales.dtos.ventas.NuevaVentaMostradorSolicitud;
import gt.cunoc.proyecto2.sales.dtos.ventas.VentaRespuesta;
import gt.cunoc.proyecto2.sales.dtos.ventas.ViajeResumenRespuesta;
import gt.cunoc.proyecto2.sales.errores.ventas.ErrorVenta;
import gt.cunoc.proyecto2.sales.errores.viajes.ErrorViaje;
import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.ClienteVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import gt.cunoc.proyecto2.sales.repositorios.asientos.AsientoViajeRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.clientes.VistaClienteRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.ventas.BoletoRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.ventas.EspecificacionesVenta;
import gt.cunoc.proyecto2.sales.repositorios.ventas.VentaRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.viajes.VistaViajeRepositorio;
import gt.cunoc.proyecto2.sales.servicios.viajes.VistaViajeServicio;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VentaServicioImpl implements VentaServicio {

    private static final Sort RECIENTES_PRIMERO = Sort.by(Sort.Order.desc("fechaPago"), Sort.Order.desc("id"));

    private final VentaRepositorio repositorio;
    private final BoletoRepositorio boletos;
    private final AsientoViajeRepositorio asientos;
    private final VistaViajeServicio viajes;
    private final VistaViajeRepositorio viajeRepositorio;
    private final VistaClienteRepositorio clientes;
    private final ClienteTrips trips;

    public VentaServicioImpl(VentaRepositorio repositorio, BoletoRepositorio boletos,
                             AsientoViajeRepositorio asientos, VistaViajeServicio viajes,
                             VistaViajeRepositorio viajeRepositorio, VistaClienteRepositorio clientes,
                             ClienteTrips trips) {
        this.repositorio = repositorio;
        this.boletos = boletos;
        this.asientos = asientos;
        this.viajes = viajes;
        this.viajeRepositorio = viajeRepositorio;
        this.clientes = clientes;
        this.trips = trips;
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
    @Transactional(readOnly = true)
    public VentaRespuesta obtener(UUID id, UsuarioActual usuario) {
        VentaEntidad venta = repositorio.findById(id)
                .orElseThrow(() -> ErrorVenta.VENTA_NO_EXISTE.excepcion());
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

    private void verificarVendible(VistaViajeEntidad viaje) {
        VendibleRespuesta respuesta = trips.consultarVendible(viaje.getViajeId());
        if (!respuesta.vendible()) {
            throw ErrorViaje.VIAJE_NO_VENDIBLE.excepcion(respuesta.motivo());
        }
    }

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