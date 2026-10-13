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

/** Ingresos por boletos y alquileres (BoletoVendido / VentaCancelada / AlquilerPagado). */
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

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(name = "fecha_negocio", nullable = false)
    private LocalDate fechaNegocio;
}
