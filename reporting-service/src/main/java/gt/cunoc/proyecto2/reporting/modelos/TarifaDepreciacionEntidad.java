package gt.cunoc.proyecto2.reporting.modelos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tarifa de depreciación por kilómetro, vigente a partir de una fecha.
 *
 * <p>Se llena con el evento que publica Identity cuando cambia la tarifa. El servicio la usa como
 * referencia; el costo de cada viaje se toma del evento de cierre que manda Trips.
 */
@Entity(name = "ReporteTarifaDepreciacion")
@Table(name = "tarifas_depreciacion")
@Getter
@Setter
@NoArgsConstructor
public class TarifaDepreciacionEntidad {

    @Id
    private UUID id;

    @Column(name = "monto_por_km", nullable = false, precision = 12, scale = 4)
    private BigDecimal montoPorKm;

    @Column(name = "vigente_desde", nullable = false, unique = true)
    private LocalDate vigenteDesde;
}
