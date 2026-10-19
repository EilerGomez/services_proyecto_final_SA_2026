package gt.cunoc.proyecto2.wallet.models;

import gt.cunoc.proyecto2.wallet.errores.ErrorCartera;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Cartera digital del usuario (una por usuario). usuarioId es el id de la cuenta en Identity.
 * La billetera del sistema (tipo SISTEMA, una sola) no tiene usuario: recibe las compras en línea
 * y paga los reembolsos.
 *
 * <p>La cartera es la experta en su saldo (GRASP Experto en información): solo ella lo suma o lo resta
 * y valida que nunca quede negativo ni pase del máximo.
 *
 * <p>Sin setters: se crea con su constructor (o {@link #delSistema()}) y el saldo solo cambia con
 * {@link #acreditar} y {@link #debitar}. El id y la versión los pone JPA; el constructor vacío es solo para JPA.
 * En la aplicación las carteras se insertan con {@code crearSiNoExiste} (idempotente).
 */
@Getter
@Entity
@Table(name = "carteras")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cartera {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    public static final String USUARIO = "USUARIO";
    public static final String SISTEMA = "SISTEMA";

    /** Mayor saldo que cabe en la columna NUMERIC(12,2). */
    public static final BigDecimal SALDO_MAXIMO = new BigDecimal("9999999999.99");

    @Column(name = "usuario_id", unique = true)
    private UUID usuarioId;

    @Column(name = "tipo", length = 10, nullable = false)
    private String tipo = USUARIO;

    @Column(name = "saldo", nullable = false)
    private BigDecimal saldo = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "fecha_actualizacion", nullable = false)
    private Instant fechaActualizacion;

    /** Suma el monto al saldo. */
    public void acreditar(BigDecimal monto) {
        saldo = saldoTras(monto);
    }

    /** Resta el monto del saldo; nunca lo deja negativo. */
    public void debitar(BigDecimal monto) {
        saldo = saldoTras(monto.negate());
    }

    /** Valida que el monto se pueda acreditar, sin cambiar el saldo (p. ej. antes de cobrar en la pasarela). */
    public void validarAcreditacion(BigDecimal monto) {
        saldoTras(monto);
    }

    /** true si el saldo alcanza para debitar el monto. */
    public boolean alcanzaPara(BigDecimal monto) {
        return saldo.compareTo(monto) >= 0;
    }

    public boolean esSistema() {
        return SISTEMA.equals(tipo);
    }

    private BigDecimal saldoTras(BigDecimal cambio) {
        BigDecimal nuevo = saldo.add(cambio);
        if (nuevo.signum() < 0) {
            // La del sistema no tiene retiros: cada devolución corresponde a un cobro previo.
            throw (esSistema() ? ErrorCartera.SALDO_SISTEMA_INSUFICIENTE : ErrorCartera.SALDO_NEGATIVO).excepcion();
        }
        if (nuevo.compareTo(SALDO_MAXIMO) > 0) {
            throw ErrorCartera.SALDO_EXCEDE_LIMITE.excepcion(SALDO_MAXIMO.toPlainString());
        }
        return nuevo;
    }

    /** Cartera de un cliente, con saldo cero. */
    public Cartera(UUID usuarioId) {
        this(usuarioId, USUARIO);
    }

    private Cartera(UUID usuarioId, String tipo) {
        this.usuarioId = usuarioId;
        this.tipo = tipo;
        this.saldo = BigDecimal.ZERO;
    }

    /** La billetera del sistema: sin usuario y con saldo cero. */
    public static Cartera delSistema() {
        return new Cartera(null, SISTEMA);
    }

    @PrePersist
    void alCrear() {
        if (fechaActualizacion == null) {
            fechaActualizacion = Instant.now();
        }
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = Instant.now();
    }
}
