package gt.cunoc.proyecto2.trips.modelos.tarifas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Tabla vista_tarifas_depreciacion (V9): copia local de las tarifas de Identity.
 * Solo lectura para JPA: la escribe el manejador de TarifaDepreciacionCambiada.
 */
@Entity(name = "VistaTarifaDepreciacion")
@Table(name = "vista_tarifas_depreciacion")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VistaTarifaDepreciacionEntidad {

    @Id
    private UUID id;

    @Column(name = "monto_por_km", nullable = false, precision = 12, scale = 4)
    private BigDecimal montoPorKm;

    @Column(name = "vigente_desde", nullable = false, unique = true)
    private LocalDate vigenteDesde;
}