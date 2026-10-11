package gt.cunoc.proyecto2.maintenance.modelos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "DetalleServicioTaller")
@Table(name = "detalle_servicio_taller")
@Getter
@Setter
@NoArgsConstructor
public class DetalleServicioEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "servicio_id", nullable = false)
    private ServicioTallerEntidad servicio;

    @Column(name = "categoria_id", nullable = false)
    private UUID categoriaId;

    /** Solo lectura (misma columna): nombre para la respuesta y el evento. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id", insertable = false, updatable = false)
    private CategoriaEntidad categoria;

    @Column(nullable = false, length = 255)
    private String descripcion;

    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;
}
