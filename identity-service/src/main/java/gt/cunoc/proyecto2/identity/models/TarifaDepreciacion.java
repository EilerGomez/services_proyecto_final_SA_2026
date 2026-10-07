package gt.cunoc.proyecto2.identity.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Tarifa de depreciación por km (solo INSERT, nunca se edita).
 */
@Getter
@Setter
@Entity
@Table(name = "tarifas_depreciacion")
public class TarifaDepreciacion {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "monto_por_km", nullable = false)
    private BigDecimal montoPorKm;

    @Column(name = "vigente_desde", nullable = false, unique = true)
    private LocalDate vigenteDesde;

    @Column(name = "creado_por", nullable = false)
    private UUID creadoPor;

    @Column(name = "fecha_creacion", nullable = false)
    private Instant fechaCreacion;
}
