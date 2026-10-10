package gt.cunoc.proyecto2.trips.modelos.sucursales;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity(name = "VistaSucursal")
@Table(name = "vista_sucursales")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VistaSucursalEntidad {

    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitud;

    @Column(nullable = false)
    private boolean activa;

    @Column(nullable = false)
    private int version;
}