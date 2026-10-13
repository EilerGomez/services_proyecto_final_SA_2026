package gt.cunoc.proyecto2.reporting.modelos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Réplica local del viaje (ViajeProgramado / ... / ViajeCompletado de Trips). */
@Entity(name = "ReporteViaje")
@Table(name = "viajes")
@Getter
@Setter
@NoArgsConstructor
public class ViajeEntidad {

    @Id
    @Column(name = "viaje_id")
    private UUID viajeId;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(name = "ruta_id")
    private UUID rutaId;

    @Column(name = "sucursal_id", nullable = false)
    private UUID sucursalId;

    @Column(name = "bus_id", nullable = false)
    private UUID busId;

    @Column(name = "chofer_id", nullable = false)
    private UUID choferId;

    @Column(name = "fecha_salida", nullable = false)
    private OffsetDateTime fechaSalida;

    @Column(name = "km_recorridos", precision = 12, scale = 2)
    private BigDecimal kmRecorridos;

    @Column(name = "tarifa_depreciacion", precision = 12, scale = 4)
    private BigDecimal tarifaDepreciacion;

    @Column(name = "costo_depreciacion", precision = 12, scale = 2)
    private BigDecimal costoDepreciacion;

    @Column(nullable = false, length = 12)
    private String estado;

    /**
     * aggregateVersion del último evento de Trips aplicado (no es @Version de JPA).
     * Se descarta el evento si su versión es menor.
     */
    @Column(nullable = false)
    private int version;
}
