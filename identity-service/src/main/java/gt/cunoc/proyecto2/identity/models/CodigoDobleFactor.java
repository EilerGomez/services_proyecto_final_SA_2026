package gt.cunoc.proyecto2.identity.models;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.Instant;
import java.util.UUID;


/**
 * Código OTP de doble factor (se guarda su hash SHA-256, un solo uso).
 */
@Getter
@Setter
@Entity
@Table(name = "codigos_doble_factor")
public class CodigoDobleFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "codigo_hash", length = 255, nullable = false)
    private String codigoHash;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "usado_en")
    private Instant usadoEn;

    @Column(name = "intentos", nullable = false)
    private int intentos = 0;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;
}
