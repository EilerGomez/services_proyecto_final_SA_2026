package gt.cunoc.proyecto2.reporting.modelos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Ingreso que alimenta los reportes de ingresos y ganancias.
 *
 * <p>Se llena con las ventas de boletos, los reembolsos por ventas canceladas y los pagos de
 * alquileres. El campo {@code tipo} indica de cuál de los tres viene cada fila.
 */
@Entity(name = "ReporteIngreso")
@Table(name = "ingresos")
@Getter
@Setter
@NoArgsConstructor
public class IngresoEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "evento_origen_id", nullable = false, unique = true)
    private UUID eventoOrigenId;

    @Column(name = "sucursal_id", nullable = false)
    private UUID sucursalId;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(name = "referencia_id", nullable = false)
    private UUID referenciaId;

    @Column(name = "viaje_id")
    private UUID viajeId;

    /** Alquiler cobrado (solo ingresos ALQUILER): por él se busca el bus que le asignaron. */
    @Column(name = "alquiler_id")
    private UUID alquilerId;

    @Column(name = "ruta_id")
    private UUID rutaId;

    @Column(name = "bus_id")
    private UUID busId;

    @Column(name = "cliente_nit", length = 20)
    private String clienteNit;

    @Column(name = "cliente_nombre", length = 201)
    private String clienteNombre;

    @Column(length = 255)
    private String origen;

    @Column(length = 255)
    private String destino;

    @Column(name = "cantidad_boletos", nullable = false)
    private int cantidadBoletos;

    /** Fechas de salida y retorno del alquiler; solo se llenan en los ingresos de tipo ALQUILER. */
    @Column(name = "fecha_salida")
    private OffsetDateTime fechaSalida;

    @Column(name = "fecha_retorno")
    private OffsetDateTime fechaRetorno;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_negocio", nullable = false)
    private LocalDate fechaNegocio;
}
