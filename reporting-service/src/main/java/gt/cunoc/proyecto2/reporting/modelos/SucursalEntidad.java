package gt.cunoc.proyecto2.reporting.modelos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Réplica local de la sucursal (SucursalCreada / SucursalActualizada de Fleet). */
@Entity(name = "ReporteSucursal")
@Table(name = "sucursales")
@Getter
@Setter
@NoArgsConstructor
public class SucursalEntidad {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitud;

    @Column(nullable = false)
    private boolean activa = true;

    /** Null al crear; se descarta el evento si su versión es <= a la guardada. */
    @Version
    private Integer version;
}
