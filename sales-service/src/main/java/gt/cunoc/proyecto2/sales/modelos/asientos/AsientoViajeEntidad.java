package gt.cunoc.proyecto2.sales.modelos.asientos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity(name = "AsientoViaje")
@Table(name = "asientos_viaje")
@IdClass(AsientoViajeId.class)
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AsientoViajeEntidad {

    @Id
    @Column(name = "viaje_id")
    private UUID viajeId;

    @Id
    @Column(name = "numero_asiento")
    private int numeroAsiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoAsiento estado;

    @Column(name = "venta_id")
    private UUID ventaId;

    @Column(name = "reservado_hasta")
    private OffsetDateTime reservadoHasta;

    @Column(nullable = false)
    private int version;

    public boolean estaOcupado(OffsetDateTime ahora) {
        return switch (estado) {
            case VENDIDO -> true;
            case RESERVADO -> reservadoHasta != null && reservadoHasta.isAfter(ahora);
            case DISPONIBLE -> false;
        };
    }
}