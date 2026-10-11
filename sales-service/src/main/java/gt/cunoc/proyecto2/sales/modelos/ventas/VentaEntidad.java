package gt.cunoc.proyecto2.sales.modelos.ventas;

import gt.cunoc.proyecto2.sales.errores.ventas.ErrorVenta;
import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tabla ventas (V5). Sin setters: cada cambio de estado es un método que valida desde dónde se llega.
 * Viaje, canal, cliente, sucursal, método y monto no cambian (updatable = false).
 * fecha_creacion y fecha_actualizacion las maneja la BD.
 */
@Entity(name = "Venta")
@Table(name = "ventas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VentaEntidad {

    @Id
    private UUID id;

    @Column(name = "viaje_id", nullable = false, updatable = false)
    private UUID viajeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private CanalVenta canal;

    @Column(name = "cliente_usuario_id", updatable = false)
    private UUID clienteUsuarioId;

    @Column(name = "cliente_nit", nullable = false, length = 20, updatable = false)
    private String clienteNit;

    @Column(name = "cliente_nombre", nullable = false, length = 201, updatable = false)
    private String clienteNombre;

    @Column(name = "cliente_correo", length = 150, updatable = false)
    private String clienteCorreo;

    @Column(name = "sucursal_id", nullable = false, updatable = false)
    private UUID sucursalId;

    @Column(name = "cajero_usuario_id", updatable = false)
    private UUID cajeroUsuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 10, updatable = false)
    private MetodoPago metodoPago;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal montoTotal;

    /** Asientos que se cobraron (V8). La saga confirma solo si siguen reservados todos. */
    @Column(name = "cantidad_boletos", nullable = false, updatable = false)
    private int cantidadBoletos;

    @Column(name = "fecha_pago", nullable = false, updatable = false)
    private LocalDate fechaPago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EstadoVenta estado;

    @Column(name = "motivo_rechazo")
    private String motivoRechazo;

    @Column(name = "motivo_cancelacion")
    private String motivoCancelacion;

    @Column(name = "fecha_cancelacion")
    private LocalDate fechaCancelacion;

    /** Null al crear (así Spring Data sabe que es nueva); Hibernate pone 0 y la sube en cada UPDATE. */
    @Version
    private Integer version;

    /**
     * S4. Venta en mostrador: efectivo, CONFIRMADA al instante.
     * El ingreso se atribuye a la sucursal de origen del viaje; monto = precio del boleto × asientos.
     */
    public static VentaEntidad registrarEnMostrador(VistaViajeEntidad viaje, ClienteVenta cliente,
                                                    int cantidadAsientos, LocalDate fechaPago, UUID cajeroId) {
        VentaEntidad venta = nueva(viaje, cliente, cantidadAsientos, fechaPago);
        venta.canal = CanalVenta.PRESENCIAL;
        venta.metodoPago = MetodoPago.EFECTIVO;
        venta.cajeroUsuarioId = cajeroId;
        venta.estado = EstadoVenta.CONFIRMADA;
        return venta;
    }

    /**
     * S2. Venta en línea: cartera, PENDIENTE_PAGO hasta que Identity responda (CarteraDebitada o
     * DebitoCarteraRechazado). El cliente es el del token (con cuenta).
     */
    public static VentaEntidad registrarEnLinea(VistaViajeEntidad viaje, ClienteVenta cliente,
                                                int cantidadAsientos, LocalDate fechaPago) {
        VentaEntidad venta = nueva(viaje, cliente, cantidadAsientos, fechaPago);
        venta.canal = CanalVenta.EN_LINEA;
        venta.metodoPago = MetodoPago.CARTERA;
        venta.estado = EstadoVenta.PENDIENTE_PAGO;
        return venta;
    }

    /**
     * S5. CONFIRMADA → CANCELADA (422 ESTADO_INVALIDO en otro estado).
     * 400 FECHA_CANCELACION_INVALIDA si la fecha es anterior al pago.
     */
    public void cancelar(String motivo, LocalDate fecha) {
        verificarEstado(EstadoVenta.CONFIRMADA);
        if (fecha.isBefore(fechaPago)) {
            throw ErrorVenta.FECHA_CANCELACION_INVALIDA.excepcion(fecha, fechaPago);
        }
        this.motivoCancelacion = motivo;
        this.fechaCancelacion = fecha;
        this.estado = EstadoVenta.CANCELADA;
    }

    /**
     * ViajeEliminado: CONFIRMADA → CANCELADA con la fecha de hoy, o con la fecha de pago si el usuario
     * eligió una fecha futura (la cancelación no puede quedar antes del pago).
     */
    public void cancelarPorViajeEliminado(String motivo, LocalDate hoy) {
        cancelar(motivo, fechaNoAnteriorAlPago(hoy));
    }

    /** CarteraDebitada (saga): PENDIENTE_PAGO → CONFIRMADA. Los boletos los emite el servicio. */
    public void confirmarPago() {
        verificarEstado(EstadoVenta.PENDIENTE_PAGO);
        this.estado = EstadoVenta.CONFIRMADA;
    }

    /**
     * PENDIENTE_PAGO → RECHAZADA: DebitoCarteraRechazado (motivo de Identity), viaje eliminado,
     * o el débito llegó cuando la venta ya había perdido sus asientos.
     */
    public void rechazar(String motivo) {
        verificarEstado(EstadoVenta.PENDIENTE_PAGO);
        this.motivoRechazo = motivo;
        this.estado = EstadoVenta.RECHAZADA;
    }

    /** CarteraReembolsada: CANCELADA o RECHAZADA → REEMBOLSADA (el dinero ya volvió a la cartera). */
    public void marcarReembolsada() {
        if (!puedeMarcarseReembolsada()) {
            throw ErrorVenta.ESTADO_INVALIDO.excepcion(estado, "CANCELADA o RECHAZADA");
        }
        this.estado = EstadoVenta.REEMBOLSADA;
    }

    public boolean puedeMarcarseReembolsada() {
        return estado == EstadoVenta.CANCELADA || estado == EstadoVenta.RECHAZADA;
    }

    public boolean estaPendienteDePago() {
        return estado == EstadoVenta.PENDIENTE_PAGO;
    }

    /** Precio de cada boleto al momento de la venta (aunque Trips cambie después el precio del viaje). */
    public BigDecimal precioPorBoleto() {
        return montoTotal.divide(BigDecimal.valueOf(cantidadBoletos), 2, RoundingMode.HALF_UP);
    }

    /** La fecha indicada, o la de pago si la indicada es anterior (reembolsos y cancelaciones automáticas). */
    public LocalDate fechaNoAnteriorAlPago(LocalDate fecha) {
        return fecha.isBefore(fechaPago) ? fechaPago : fecha;
    }

    public boolean pagadaConCartera() {
        return metodoPago == MetodoPago.CARTERA;
    }

    /** Puede verla su cliente (si tiene cuenta). El personal se revisa en el servicio. */
    public boolean esDelCliente(UUID usuarioId) {
        return clienteUsuarioId != null && clienteUsuarioId.equals(usuarioId);
    }

    private void verificarEstado(EstadoVenta requerido) {
        if (estado != requerido) {
            throw ErrorVenta.ESTADO_INVALIDO.excepcion(estado, requerido);
        }
    }

    private static VentaEntidad nueva(VistaViajeEntidad viaje, ClienteVenta cliente, int cantidadAsientos,
                                      LocalDate fechaPago) {
        VentaEntidad venta = new VentaEntidad();
        venta.id = UUID.randomUUID();
        venta.viajeId = viaje.getViajeId();
        venta.sucursalId = viaje.getSucursalOrigenId();
        venta.clienteUsuarioId = cliente.usuarioId();
        venta.clienteNit = cliente.nit();
        venta.clienteNombre = cliente.nombre();
        venta.clienteCorreo = cliente.correo();
        venta.montoTotal = viaje.getPrecioBoleto().multiply(BigDecimal.valueOf(cantidadAsientos));
        venta.cantidadBoletos = cantidadAsientos;
        venta.fechaPago = fechaPago;
        return venta;
    }
}
