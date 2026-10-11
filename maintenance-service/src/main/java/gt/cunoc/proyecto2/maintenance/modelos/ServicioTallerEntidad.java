package gt.cunoc.proyecto2.maintenance.modelos;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(name = "ServicioTaller")
@Table(name = "servicios_taller")
@Getter
@Setter
@NoArgsConstructor
public class ServicioTallerEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "bus_id", nullable = false)
    private UUID busId;

    @Column(name = "sucursal_id", nullable = false)
    private UUID sucursalId;

    @Column(name = "fecha_servicio", nullable = false)
    private LocalDate fechaServicio;

    @Column(length = 150)
    private String taller;

    @Column(length = 500)
    private String observaciones;

    @Column(name = "total_mano_obra", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalManoObra = BigDecimal.ZERO;

    @Column(name = "total_repuestos", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalRepuestos = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "registrado_por", nullable = false)
    private UUID registradoPor;

    @OneToMany(mappedBy = "servicio", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("id")
    private List<DetalleServicioEntidad> lineas = new ArrayList<>();

    public void agregarLinea(DetalleServicioEntidad linea) {
        linea.setServicio(this);
        lineas.add(linea);
    }
}
