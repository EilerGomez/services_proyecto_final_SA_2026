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
import java.time.OffsetDateTime;
import java.util.Locale;
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

    /** Caracteres del id que forman el código del boleto impreso (48 bits). */
    private static final int LARGO_CODIGO = 12;

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

    /** 500 (V11): al eliminarse un viaje lleva el origen, el destino, la salida y la placa del bus. */
    @Column(name = "motivo_cancelacion", length = 500)
    private String motivoCancelacion;

    @Column(name = "fecha_cancelacion")
    private LocalDate fechaCancelacion;

    /** V11: cuándo volvió el dinero (la cartera lo devuelve sola; el efectivo lo entrega la sucursal). */
    @Column(name = "fecha_reembolso")
    private LocalDate fechaReembolso;

    /** V11: cajero o administrador que entregó el efectivo. Null si lo devolvió la cartera. */
    @Column(name = "reembolsado_por")
    private UUID reembolsadoPor;

    /** Momento en que se registró (DEFAULT now() en la BD): ordena las ventas del mismo día de pago. */
    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

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
     * S2. Venta en línea: cartera, PENDIENTE_PAGO hasta que Wallet responda (CarteraDebitada o
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
     * PENDIENTE_PAGO → RECHAZADA: DebitoCarteraRechazado (motivo de Wallet), viaje eliminado,
     * o el débito llegó cuando la venta ya había perdido sus asientos.
     */
    public void rechazar(String motivo) {
        verificarEstado(EstadoVenta.PENDIENTE_PAGO);
        this.motivoRechazo = motivo;
        this.estado = EstadoVenta.RECHAZADA;
    }

    /** CarteraReembolsada: CANCELADA o RECHAZADA → REEMBOLSADA (el dinero ya volvió a la cartera). */
    public void marcarReembolsada(LocalDate fecha) {
        verificarPuedeReembolsarse();
        this.fechaReembolso = fechaNoAnteriorAlPago(fecha);
        this.estado = EstadoVenta.REEMBOLSADA;
    }

    /**
     * S14. Devolución del efectivo en la sucursal: CANCELADA o RECHAZADA → REEMBOLSADA y queda
     * registrado quién entregó el dinero. Solo para ventas que se pagaron en efectivo: lo que se pagó
     * con cartera lo devuelve Wallet.
     */
    public void reembolsarEnEfectivo(LocalDate fecha, UUID porUsuarioId) {
        if (pagadaConCartera()) {
            throw ErrorVenta.REEMBOLSO_NO_EN_EFECTIVO.excepcion();
        }
        verificarPuedeReembolsarse();
        this.fechaReembolso = fechaNoAnteriorAlPago(fecha);
        this.reembolsadoPor = porUsuarioId;
        this.estado = EstadoVenta.REEMBOLSADA;
    }

    public boolean puedeMarcarseReembolsada() {
        return estado == EstadoVenta.CANCELADA || estado == EstadoVenta.RECHAZADA;
    }

    public boolean estaReembolsada() {
        return estado == EstadoVenta.REEMBOLSADA;
    }

    /**
     * Código del boleto impreso: los primeros 12 caracteres del id, en mayúsculas. Es lo que lleva el
     * código de barras del talón, para localizar la venta cuando el cliente llega con el boleto en mano.
     */
    public String codigo() {
        return codigoDe(id);
    }

    public static String codigoDe(UUID id) {
        return id == null ? null : id.toString().replace("-", "").substring(0, LARGO_CODIGO).toUpperCase(Locale.ROOT);
    }

    /**
     * Deja el código como se guarda: sin guiones ni espacios, en mayúsculas y recortado a 12.
     * Así funciona igual si se escanea el código del talón, si se escribe con guiones o si se pega
     * el id completo de la venta. Devuelve null si no quedan 12 caracteres hexadecimales.
     */
    public static String normalizarCodigo(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.replaceAll("[^0-9A-Fa-f]", "").toUpperCase(Locale.ROOT);
        return limpio.length() < LARGO_CODIGO ? null : limpio.substring(0, LARGO_CODIGO);
    }

    private void verificarPuedeReembolsarse() {
        if (estaReembolsada()) {
            throw ErrorVenta.VENTA_YA_REEMBOLSADA.excepcion();
        }
        if (!puedeMarcarseReembolsada()) {
            throw ErrorVenta.ESTADO_INVALIDO.excepcion(estado, "CANCELADA o RECHAZADA");
        }
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
