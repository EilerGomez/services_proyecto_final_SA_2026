package gt.cunoc.proyecto2.sales.modelos.viajes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity(name = "VistaViaje")
@Table(name = "vista_viajes")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VistaViajeEntidad {

    @Id
    @Column(name = "viaje_id")
    private UUID viajeId;

    @Column(name = "ruta_id", nullable = false)
    private UUID rutaId;

    @Column(name = "sucursal_origen_id", nullable = false)
    private UUID sucursalOrigenId;

    @Column(name = "origen_nombre", nullable = false, length = 100)
    private String origenNombre;

    @Column(name = "destino_nombre", nullable = false, length = 100)
    private String destinoNombre;

    @Column(name = "fecha_salida", nullable = false)
    private OffsetDateTime fechaSalida;

    @Column(name = "bus_id", nullable = false)
    private UUID busId;

    @Column(name = "bus_placa", length = 10)
    private String busPlaca;

    @Column(nullable = false)
    private int capacidad;

    @Column(name = "precio_boleto", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBoleto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoViaje estado;

    @Column(nullable = false)
    private int version;

    public boolean estaALaVenta() {
        return estado == EstadoViaje.PROGRAMADO;
    }
}