package gt.cunoc.proyecto2.reporting.modelos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Gasto operativo que alimenta los reportes de gastos y ganancias.
 *
 * <p>Se llena con los costos de cada viaje completado (combustible, depreciación y salario) y con
 * cada línea de un servicio de taller. El campo {@code tipo} distingue el origen del gasto.
 */
@Entity(name = "ReporteGasto")
@Table(name = "gastos")
@Getter
@Setter
@NoArgsConstructor
public class GastoEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "evento_origen_id", nullable = false)
    private UUID eventoOrigenId;

    @Column(name = "sucursal_id", nullable = false)
    private UUID sucursalId;

    @Column(nullable = false, length = 15)
    private String tipo;

    @Column(name = "bus_id", nullable = false)
    private UUID busId;

    @Column(name = "viaje_id")
    private UUID viajeId;

    @Column(name = "servicio_taller_id")
    private UUID servicioTallerId;

    @Column(name = "linea_detalle_id")
    private UUID lineaDetalleId;

    @Column(name = "categoria_id")
    private UUID categoriaId;

    @Column(name = "categoria_nombre", length = 60)
    private String categoriaNombre;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_negocio", nullable = false)
    private LocalDate fechaNegocio;
}
