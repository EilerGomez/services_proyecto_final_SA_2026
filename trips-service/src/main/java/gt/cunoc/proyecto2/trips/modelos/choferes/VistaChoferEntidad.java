package gt.cunoc.proyecto2.trips.modelos.choferes;

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

/**
 * Tabla vista_choferes (V6): copia local de los choferes de Fleet.
 */
@Entity(name = "VistaChofer")
@Table(name = "vista_choferes")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VistaChoferEntidad {

    @Id
    private UUID id;

    @Column(name = "sucursal_id", nullable = false)
    private UUID sucursalId;

    @Column(name = "nombre_completo", nullable = false, length = 201)
    private String nombreCompleto;

    @Column(name = "salario_base_viaje", nullable = false, precision = 12, scale = 2)
    private BigDecimal salarioBaseViaje;

    @Column(nullable = false, length = 10)
    private String estado;

    /** V19: su cuenta en Identity, para poder avisarle por correo cuando se le asigna un viaje. */
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(nullable = false)
    private int version;
}