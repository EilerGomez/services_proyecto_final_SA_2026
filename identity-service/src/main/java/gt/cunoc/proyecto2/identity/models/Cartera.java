package gt.cunoc.proyecto2.identity.models;

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
 * Cartera digital del usuario (una por usuario).
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

    @Column(name = "usuario_id", nullable = false, unique = true)
    private UUID usuarioId;

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
