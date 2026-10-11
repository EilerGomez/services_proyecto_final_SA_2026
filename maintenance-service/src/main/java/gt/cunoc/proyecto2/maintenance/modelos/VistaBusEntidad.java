package gt.cunoc.proyecto2.maintenance.modelos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Proyección local de buses (BusCreado / BusActualizado / BusDesactivado de Fleet).
 * {@code version} es la versión del productor (columna manual, sin {@code @Version}).
 */
@Entity(name = "VistaBus")
@Table(name = "vista_buses")
@Getter
@Setter
@NoArgsConstructor
public class VistaBusEntidad {

    @Id
    @Column(name = "bus_id")
    private UUID busId;

    @Column(nullable = false, length = 10)
    private String placa;

    @Column(name = "sucursal_id", nullable = false)
    private UUID sucursalId;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(nullable = false)
    private Integer version = 0;
}
