package gt.cunoc.proyecto2.wallet.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Movimiento de cartera. Del cliente: RECARGA, DEBITO o REEMBOLSO. De la billetera del sistema:
 * COBRO (entra el pago de una compra en línea) y DEVOLUCION (sale el reembolso de esa compra).
 *
 * <p>{@code UNIQUE(tipo, referencia_id)} hace idempotente la saga y
 * {@code UNIQUE(proveedor_pago, referencia_externa)} evita acreditar dos veces el mismo cobro de la pasarela.
 *
 * <p>Se crea solo con sus métodos de fábrica ({@link #recarga}, {@link #debito}, {@link #reembolso},
 * {@link #cobro}, {@link #devolucion}), que reciben la {@link Cartera} ya con el saldo aplicado: de ahí
 * toman la cartera y el saldo resultante; todos llaman al constructor privado. Es inmutable (sin setters):
 * un movimiento no se edita.
 * El constructor vacío es solo para JPA.
 */
@Getter
@Entity
@Table(name = "movimientos_cartera")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MovimientoCartera {

    public static final String RECARGA = "RECARGA";
    public static final String DEBITO = "DEBITO";
    public static final String REEMBOLSO = "REEMBOLSO";
    public static final String COBRO = "COBRO";
    public static final String DEVOLUCION = "DEVOLUCION";

    /** Tipo de referencia de las recargas: no vienen de una venta ni de un alquiler. */
    public static final String MANUAL = "MANUAL";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "cartera_id", nullable = false)
    private UUID carteraId;

    @Column(name = "tipo", length = 10, nullable = false)
    private String tipo;

    @Column(name = "monto", nullable = false)
    private BigDecimal monto;

    @Column(name = "saldo_resultante", nullable = false)
    private BigDecimal saldoResultante;

    @Column(name = "tipo_referencia", length = 10, nullable = false)
    private String tipoReferencia;

    @Column(name = "referencia_id")
    private UUID referenciaId;

    @Column(name = "proveedor_pago", length = 30)
    private String proveedorPago;

    @Column(name = "referencia_externa", length = 100)
    private String referenciaExterna;

    @Column(name = "fecha_movimiento", nullable = false)
    private LocalDate fechaMovimiento;

    /** Momento real en que se registró: ordena los movimientos del mismo día (el más reciente primero). */
    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private Instant fechaCreacion;

    /**
     * Único constructor: todo el movimiento se arma aquí a partir de la cartera (su id y el saldo que quedó).
     * Sin fecha se usa la de hoy.
     */
    private MovimientoCartera(Cartera cartera, String tipo, BigDecimal monto, String tipoReferencia,
                              UUID referenciaId, String proveedorPago, String referenciaExterna, LocalDate fecha) {
        this.carteraId = cartera.getId();
        this.tipo = tipo;
        this.monto = monto;
        this.saldoResultante = cartera.getSaldo();
        this.tipoReferencia = tipoReferencia;
        this.referenciaId = referenciaId;
        this.proveedorPago = proveedorPago;
        this.referenciaExterna = referenciaExterna;
        this.fechaMovimiento = fecha == null ? LocalDate.now() : fecha;
        this.fechaCreacion = Instant.now();
    }

    /** Movimientos de una venta o un alquiler: no pasan por la pasarela. */
    private MovimientoCartera(Cartera cartera, String tipo, BigDecimal monto, String tipoReferencia,
                              UUID referenciaId, LocalDate fecha) {
        this(cartera, tipo, monto, tipoReferencia, referenciaId, null, null, fecha);
    }

    /** Recarga cobrada en la pasarela de pagos. */
    public static MovimientoCartera recarga(Cartera cartera, BigDecimal monto, String proveedorPago,
                                            String referenciaExterna, LocalDate fecha) {
        return new MovimientoCartera(cartera, RECARGA, monto, MANUAL, null, proveedorPago, referenciaExterna, fecha);
    }

    /** Pago de una venta o un alquiler con la cartera del cliente. */
    public static MovimientoCartera debito(Cartera cartera, BigDecimal monto, String tipoReferencia,
                                           UUID referenciaId, LocalDate fecha) {
        return new MovimientoCartera(cartera, DEBITO, monto, tipoReferencia, referenciaId, fecha);
    }

    /** Devolución al cliente de una venta o un alquiler cancelado. */
    public static MovimientoCartera reembolso(Cartera cartera, BigDecimal monto, String tipoReferencia,
                                              UUID referenciaId, LocalDate fecha) {
        return new MovimientoCartera(cartera, REEMBOLSO, monto, tipoReferencia, referenciaId, fecha);
    }

    /** Entrada a la billetera del sistema del pago de una compra en línea. */
    public static MovimientoCartera cobro(Cartera sistema, BigDecimal monto, String tipoReferencia,
                                          UUID referenciaId, LocalDate fecha) {
        return new MovimientoCartera(sistema, COBRO, monto, tipoReferencia, referenciaId, fecha);
    }

    /** Salida de la billetera del sistema por el reembolso de una compra. */
    public static MovimientoCartera devolucion(Cartera sistema, BigDecimal monto, String tipoReferencia,
                                               UUID referenciaId, LocalDate fecha) {
        return new MovimientoCartera(sistema, DEVOLUCION, monto, tipoReferencia, referenciaId, fecha);
    }
}
