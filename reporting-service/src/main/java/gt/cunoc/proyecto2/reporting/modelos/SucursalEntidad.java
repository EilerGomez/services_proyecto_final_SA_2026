package gt.cunoc.proyecto2.reporting.modelos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Copia local de una sucursal, usada en todos los reportes por sucursal y en el mapa de rutas.
 *
 * <p>Se alimenta con los eventos de creación y actualización de Fleet. Las coordenadas permiten
 * ubicarla en el mapa.
 */
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

    /**
     * aggregateVersion del último evento de Fleet aplicado (no es @Version de JPA).
     * Se descarta el evento si su versión es menor.
     */
    @Column(nullable = false)
    private int version;
}
