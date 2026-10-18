package gt.cunoc.proyecto2.wallet.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Movimiento de cartera (RECARGA, DEBITO o REEMBOLSO).
 *
 * <p>{@code UNIQUE(tipo, referencia_id)} hace idempotente la saga y
 * {@code UNIQUE(proveedor_pago, referencia_externa)} evita acreditar dos veces el mismo cobro de la pasarela.
 */
@Getter
@Setter
@Entity
@Table(name = "movimientos_cartera")
public class MovimientoCartera {

    public static final String RECARGA = "RECARGA";
    public static final String DEBITO = "DEBITO";
    public static final String REEMBOLSO = "REEMBOLSO";

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
}
