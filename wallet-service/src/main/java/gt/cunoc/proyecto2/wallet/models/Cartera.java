package gt.cunoc.proyecto2.wallet.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Cartera digital del usuario (una por usuario). usuarioId es el id de la cuenta en Identity.
 * La billetera del sistema (tipo SISTEMA, una sola) no tiene usuario: recibe las compras en línea
 * y paga los reembolsos.
 */
@Getter
@Setter
@Entity
@Table(name = "carteras")
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
