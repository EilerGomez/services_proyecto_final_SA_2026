package gt.cunoc.proyecto2.reporting.modelos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Copia local de un chofer, usada por los reportes de personal.
 *
 * <p>Se alimenta con los eventos de creación, actualización y desactivación de Fleet. Los reportes
 * leen de aquí la licencia, su vencimiento y el total de viajes completados.
 */
@Entity(name = "ReporteChofer")
@Table(name = "choferes")
@Getter
@Setter
@NoArgsConstructor
public class ChoferEntidad {

    @Id
    private UUID id;

    @Column(name = "sucursal_id", nullable = false)
    private UUID sucursalId;

    @Column(name = "nombre_completo", nullable = false, length = 201)
    private String nombreCompleto;

    @Column(name = "numero_licencia", nullable = false, length = 30)
    private String numeroLicencia;

    @Column(name = "tipo_licencia", nullable = false, length = 2)
    private String tipoLicencia;

    @Column(name = "fecha_vencimiento_licencia", nullable = false)
    private LocalDate fechaVencimientoLicencia;

    @Column(nullable = false, length = 10)
    private String estado;

    @Column(name = "total_viajes", nullable = false)
    private int totalViajes = 0;

    /**
     * aggregateVersion del último evento de Fleet aplicado (no es @Version de JPA): los cambios
     * locales (total_viajes) no la tocan. Se descarta el evento si su versión es menor.
     */
    @Column(nullable = false)
    private int version;
}
