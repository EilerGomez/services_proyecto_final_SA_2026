package gt.cunoc.proyecto2.sales;

import gt.cunoc.proyecto2.comun.seguridad.users.Roles;
import gt.cunoc.proyecto2.comun.seguridad.users.UsuarioActual;
import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import gt.cunoc.proyecto2.sales.modelos.alquileres.RecorridoOrden;
import gt.cunoc.proyecto2.sales.modelos.asientos.AsientoViajeEntidad;
import gt.cunoc.proyecto2.sales.modelos.asientos.EstadoAsiento;
import gt.cunoc.proyecto2.sales.modelos.clientes.VistaClienteEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import gt.cunoc.proyecto2.sales.modelos.ventas.ClienteVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import gt.cunoc.proyecto2.sales.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Datos de prueba reutilizables (entidades, vistas, usuarios y fechas).
 * Las entidades salen con version 0, como si ya estuvieran guardadas.
 */
public final class DatosPrueba {

    public static final UUID XELA = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID GUATE = UUID.fromString("22222222-2222-2222-2222-222222222222");

    /** Fecha de pago elegida por el usuario. */
    public static final LocalDate FECHA_PAGO = LocalDate.of(2026, 11, 1);
    /** 5-nov 08:00 en Guatemala. */
    public static final OffsetDateTime SALIDA = OffsetDateTime.parse("2026-11-05T08:00:00-06:00");
    /** 8-nov 18:00 en Guatemala (retorno del alquiler). */
    public static final OffsetDateTime RETORNO = OffsetDateTime.parse("2026-11-08T18:00:00-06:00");
    public static final BigDecimal PRECIO_BOLETO = new BigDecimal("75.00");
    /** Puntos del mapa del alquiler: Xela → Antigua. */
    public static final RecorridoOrden RECORRIDO = new RecorridoOrden(new BigDecimal("14.845200"),
            new BigDecimal("-91.518100"), new BigDecimal("14.561400"), new BigDecimal("-90.734400"));

    private DatosPrueba() {
    }

    // ----- Usuarios (claims del JWT) -----

    public static UsuarioActual adminSistema() {
        return new UsuarioActual(UUID.randomUUID(), "admin@sistema.gt", Set.of(Roles.ADMIN_SISTEMA), null, null);
    }

    public static UsuarioActual adminSucursal(UUID sucursalId) {
        return new UsuarioActual(UUID.randomUUID(), "admin@sucursal.gt", Set.of(Roles.ADMIN_SUCURSAL),
                sucursalId, null);
    }

    public static UsuarioActual cajero(UUID sucursalId) {
        return new UsuarioActual(UUID.randomUUID(), "cajero@sucursal.gt", Set.of(Roles.CAJERO), sucursalId, null);
    }

    /** Piloto: no tiene sucursal propia, su permiso para vender sale del viaje que maneja. */
    public static UsuarioActual chofer(UUID choferId) {
        return new UsuarioActual(UUID.randomUUID(), "piloto@sucursal.gt", Set.of(Roles.CHOFER), null, choferId);
    }

    public static UsuarioActual cliente(UUID usuarioId) {
        return new UsuarioActual(usuarioId, "cliente@correo.com", Set.of(Roles.CLIENTE), null, null);
    }

    public static UsuarioActual cliente() {
        return cliente(UUID.randomUUID());
    }

    // ----- Vistas (copias de Trips e Identity): sin fábrica, se llenan por reflexión -----

    /** Viaje REGULAR Xela → Guate del 5-nov, bus P-123ABC, Q75, version 0. */
    public static VistaViajeEntidad vistaViaje(UUID viajeId, UUID sucursalOrigenId, int capacidad,
                                               EstadoViaje estado) {
        VistaViajeEntidad viaje = BeanUtils.instantiateClass(VistaViajeEntidad.class);
        ReflectionTestUtils.setField(viaje, "viajeId", viajeId);
        ReflectionTestUtils.setField(viaje, "rutaId", UUID.randomUUID());
        ReflectionTestUtils.setField(viaje, "sucursalOrigenId", sucursalOrigenId);
        ReflectionTestUtils.setField(viaje, "origenNombre", "Quetzaltenango");
        ReflectionTestUtils.setField(viaje, "destinoNombre", "Guatemala");
        ReflectionTestUtils.setField(viaje, "fechaSalida", SALIDA);
        ReflectionTestUtils.setField(viaje, "busId", UUID.randomUUID());
        ReflectionTestUtils.setField(viaje, "busPlaca", "P-123ABC");
        ReflectionTestUtils.setField(viaje, "capacidad", capacidad);
        ReflectionTestUtils.setField(viaje, "precioBoleto", PRECIO_BOLETO);
        ReflectionTestUtils.setField(viaje, "estado", estado);
        ReflectionTestUtils.setField(viaje, "version", 0);
        return viaje;
    }

    /** Viaje PROGRAMADO de 40 asientos que sale de Xela. */
    public static VistaViajeEntidad vistaViaje() {
        return vistaViaje(UUID.randomUUID(), XELA, 40, EstadoViaje.PROGRAMADO);
    }

    public static VistaClienteEntidad vistaCliente(UUID usuarioId, String nit, String nombre, String correo) {
        VistaClienteEntidad cliente = BeanUtils.instantiateClass(VistaClienteEntidad.class);
        ReflectionTestUtils.setField(cliente, "usuarioId", usuarioId);
        ReflectionTestUtils.setField(cliente, "nit", nit);
        ReflectionTestUtils.setField(cliente, "nombre", nombre);
        ReflectionTestUtils.setField(cliente, "correo", correo);
        return cliente;
    }

    /** Cliente "Ana López", NIT 1234567-8. */
    public static VistaClienteEntidad vistaCliente(UUID usuarioId) {
        return vistaCliente(usuarioId, "1234567-8", "Ana López", "ana@correo.com");
    }

    public static AsientoViajeEntidad asiento(UUID viajeId, int numero, EstadoAsiento estado, UUID ventaId,
                                              OffsetDateTime reservadoHasta) {
        AsientoViajeEntidad asiento = BeanUtils.instantiateClass(AsientoViajeEntidad.class);
        ReflectionTestUtils.setField(asiento, "viajeId", viajeId);
        ReflectionTestUtils.setField(asiento, "numeroAsiento", numero);
        ReflectionTestUtils.setField(asiento, "estado", estado);
        ReflectionTestUtils.setField(asiento, "ventaId", ventaId);
        ReflectionTestUtils.setField(asiento, "reservadoHasta", reservadoHasta);
        ReflectionTestUtils.setField(asiento, "version", 0);
        return asiento;
    }

    // ----- Ventas y boletos -----

    public static ClienteVenta clienteConCuenta(UUID usuarioId) {
        return new ClienteVenta(usuarioId, "1234567-8", "Ana López", "ana@correo.com");
    }

    /** Venta en mostrador CONFIRMADA en efectivo, consumidor final, version 0. */
    public static VentaEntidad ventaMostrador(VistaViajeEntidad viaje, int asientos) {
        VentaEntidad venta = VentaEntidad.registrarEnMostrador(viaje, new ClienteVenta(null, null, null, null),
                asientos, FECHA_PAGO, UUID.randomUUID());
        return conVersion(venta, 0);
    }

    /** Venta en línea PENDIENTE_PAGO con cartera del cliente, version 0. */
    public static VentaEntidad ventaEnLinea(VistaViajeEntidad viaje, UUID clienteUsuarioId, int asientos) {
        VentaEntidad venta = VentaEntidad.registrarEnLinea(viaje, clienteConCuenta(clienteUsuarioId), asientos,
                FECHA_PAGO);
        return conVersion(venta, 0);
    }

    /** Venta en línea ya CONFIRMADA (CarteraDebitada). */
    public static VentaEntidad ventaEnLineaConfirmada(VistaViajeEntidad viaje, UUID clienteUsuarioId, int asientos) {
        VentaEntidad venta = ventaEnLinea(viaje, clienteUsuarioId, asientos);
        venta.confirmarPago();
        return venta;
    }

    /** Boletos ACTIVO de la venta, uno por número, con id (como si ya se hubieran guardado). */
    public static List<BoletoEntidad> boletos(VentaEntidad venta, Integer... numeros) {
        return Arrays.stream(numeros)
                .map(numero -> {
                    BoletoEntidad boleto = BoletoEntidad.emitir(venta, numero, venta.precioPorBoleto());
                    ReflectionTestUtils.setField(boleto, "id", UUID.randomUUID());
                    return boleto;
                })
                .toList();
    }

    // ----- Órdenes de alquiler -----

    /** Orden PENDIENTE_PAGO de Q2600 (Xela → Antigua). clienteUsuarioId null = alquiler de mostrador. */
    public static OrdenAlquilerEntidad orden(UUID clienteUsuarioId) {
        ClienteVenta cliente = new ClienteVenta(clienteUsuarioId, "1234567-8", "Colegio San José",
                "colegio@correo.com");
        OrdenAlquilerEntidad orden = OrdenAlquilerEntidad.registrar(UUID.randomUUID(), XELA, cliente,
                "Quetzaltenango", "Antigua Guatemala", RECORRIDO, SALIDA, RETORNO, new BigDecimal("2600.00"));
        return conVersion(orden, 0);
    }

    /** Pone la version que tendría la fila en la BD (las entidades no tienen setter de version). */
    public static <T> T conVersion(T entidad, int version) {
        ReflectionTestUtils.setField(entidad, "version", version);
        return entidad;
    }
}
