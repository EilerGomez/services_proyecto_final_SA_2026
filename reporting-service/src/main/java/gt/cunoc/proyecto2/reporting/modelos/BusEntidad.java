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
 * Copia local de un bus, usada por los reportes de flota.
 *
 * <p>Se alimenta con los eventos de creación, actualización y desactivación de Fleet. Además de los
 * datos del bus guarda dos valores que calcula el propio servicio de reportes: el chofer que tiene
 * asignado y el total de viajes completados.
 */
@Entity(name = "ReporteBus")
@Table(name = "buses")
@Getter
@Setter
@NoArgsConstructor
public class BusEntidad {

    @Id
    private UUID id;

    @Column(name = "sucursal_id", nullable = false)
    private UUID sucursalId;

    @Column(nullable = false, length = 10)
    private String placa;

    @Column(nullable = false, length = 50)
    private String marca;

    @Column(nullable = false, length = 50)
    private String modelo;

    @Column(name = "capacidad_pasajeros", nullable = false)
    private int capacidadPasajeros;

    @Column(nullable = false, length = 15)
    private String estado;

    @Column(name = "kilometraje_actual", nullable = false, precision = 12, scale = 2)
    private BigDecimal kilometrajeActual = BigDecimal.ZERO;

    @Column(name = "chofer_actual_id")
    private UUID choferActualId;

    @Column(name = "total_viajes", nullable = false)
    private int totalViajes = 0;

    /**
     * aggregateVersion del último evento de Fleet aplicado (no es @Version de JPA): los cambios
     * locales (total_viajes, chofer_actual_id) no la tocan. Se descarta el evento si su versión es menor.
     */
    @Column(nullable = false)
    private int version;
}
