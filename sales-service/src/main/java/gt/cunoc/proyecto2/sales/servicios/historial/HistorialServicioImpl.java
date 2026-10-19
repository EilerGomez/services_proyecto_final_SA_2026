package gt.cunoc.proyecto2.sales.servicios.historial;

import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.dtos.historial.AlquilerHistorialRespuesta;
import gt.cunoc.proyecto2.sales.dtos.historial.HistorialRespuesta;
import gt.cunoc.proyecto2.sales.dtos.historial.VentaHistorialRespuesta;
import gt.cunoc.proyecto2.sales.errores.historial.ErrorHistorial;
import gt.cunoc.proyecto2.sales.errores.ventas.ErrorVenta;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import gt.cunoc.proyecto2.sales.modelos.clientes.VistaClienteEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.ClienteVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import gt.cunoc.proyecto2.sales.repositorios.alquileres.OrdenAlquilerRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.clientes.VistaClienteRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.ventas.BoletoRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.ventas.VentaRepositorio;
import gt.cunoc.proyecto2.sales.repositorios.viajes.VistaViajeRepositorio;
import gt.cunoc.proyecto2.sales.utilidades.Normalizador;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistorialServicioImpl implements HistorialServicio {

    private final VistaClienteRepositorio clientes;
    private final VentaRepositorio ventas;
    private final BoletoRepositorio boletos;
    private final VistaViajeRepositorio viajes;
    private final OrdenAlquilerRepositorio ordenes;

    public HistorialServicioImpl(VistaClienteRepositorio clientes, VentaRepositorio ventas, BoletoRepositorio boletos,
                                 VistaViajeRepositorio viajes, OrdenAlquilerRepositorio ordenes) {
        this.clientes = clientes;
        this.ventas = ventas;
        this.boletos = boletos;
        this.viajes = viajes;
        this.ordenes = ordenes;
    }

    @Override
    @Transactional(readOnly = true)
    public HistorialRespuesta delCliente(UsuarioActual usuario) {
        VistaClienteEntidad perfil = clientes.findById(usuario.usuarioId())
                .orElseThrow(() -> ErrorVenta.CLIENTE_SIN_PERFIL.excepcion());
        return armar(perfil.getNit(), Optional.of(perfil));
    }

    @Override
    @Transactional(readOnly = true)
    public HistorialRespuesta porNit(String nit) {
        String limpio = Normalizador.codigo(nit);
        if (ClienteVenta.CONSUMIDOR_FINAL_NIT.equals(limpio)) {
            throw ErrorHistorial.NIT_CONSUMIDOR_FINAL.excepcion();
        }
        return armar(limpio, clientes.findFirstByNit(limpio));
    }

    /** Carga ventas, boletos, viajes y órdenes del NIT con una consulta por tabla. */
    private HistorialRespuesta armar(String nit, Optional<VistaClienteEntidad> perfil) {
        List<VentaEntidad> suyas = ventas.findByClienteNit(nit, VentaRepositorio.RECIENTES_PRIMERO);
        List<OrdenAlquilerEntidad> alquileres = ordenes.findByClienteNitOrderByFechaCreacionDesc(nit);

        List<UUID> ventaIds = suyas.stream().map(VentaEntidad::getId).toList();
        Map<UUID, List<BoletoEntidad>> boletosPorVenta = ventaIds.isEmpty() ? Map.of()
                : boletos.findByVentaIdIn(ventaIds).stream().collect(Collectors.groupingBy(BoletoEntidad::getVentaId));
        Map<UUID, VistaViajeEntidad> viajesPorId = viajes
                .findAllById(suyas.stream().map(VentaEntidad::getViajeId).distinct().toList()).stream()
                .collect(Collectors.toMap(VistaViajeEntidad::getViajeId, Function.identity()));

        List<VentaHistorialRespuesta> compras = suyas.stream()
                .map(venta -> new VentaHistorialRespuesta(venta, viajesPorId.get(venta.getViajeId()),
                        boletosPorVenta.getOrDefault(venta.getId(), List.of())))
                .toList();
        return new HistorialRespuesta(nit, nombre(perfil, suyas, alquileres), compras,
                alquileres.stream().map(AlquilerHistorialRespuesta::new).toList());
    }

    private static String nombre(Optional<VistaClienteEntidad> perfil, List<VentaEntidad> suyas,
                                 List<OrdenAlquilerEntidad> alquileres) {
        if (perfil.isPresent()) {
            return perfil.get().getNombre();
        }
        if (!suyas.isEmpty()) {
            return suyas.get(0).getClienteNombre();
        }
        return alquileres.isEmpty() ? null : alquileres.get(0).getClienteNombre();
    }
}