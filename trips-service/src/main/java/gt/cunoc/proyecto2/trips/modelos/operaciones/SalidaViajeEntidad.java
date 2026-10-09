package gt.cunoc.proyecto2.trips.modelos.operaciones;

import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.springframework.data.domain.Persistable;

/**
 * Tabla salidas_viaje (V8): la salida de un viaje (T14).
 * El id es el del viaje; Persistable indica a Spring Data que es nueva y hace INSERT directo
 * (sin el SELECT previo de merge, porque el id ya viene asignado).
 * fecha_registro la pone la BD.
 */
@Entity(name = "SalidaViaje")
@Table(name = "salidas_viaje")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SalidaViajeEntidad implements Persistable<UUID> {

    @Id
    @Column(name = "viaje_id")
    private UUID viajeId;

    @Column(name = "bus_id", nullable = false)
    private UUID busId;

    @Column(name = "chofer_id", nullable = false)
    private UUID choferId;

    @Column(name = "fecha_hora_salida", nullable = false)
    private OffsetDateTime fechaHoraSalida;

    @Column(name = "km_inicial", nullable = false, precision = 12, scale = 2)
    private BigDecimal kmInicial;

    @Column(name = "registrado_por", nullable = false)
    private UUID registradoPor;

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean nueva;

    /** Copia el bus y el chofer que tiene el viaje al salir. */
    public static SalidaViajeEntidad registrar(ViajeEntidad viaje, OffsetDateTime fechaHoraSalida,
                                               BigDecimal kmInicial, UUID registradoPor) {
        SalidaViajeEntidad salida = new SalidaViajeEntidad();
        salida.viajeId = viaje.getId();
        salida.busId = viaje.getBusId();
        salida.choferId = viaje.getChoferId();
        salida.fechaHoraSalida = fechaHoraSalida;
        salida.kmInicial = kmInicial;
        salida.registradoPor = registradoPor;
        salida.nueva = true;
        return salida;
    }

    @Override
    public UUID getId() {
        return viajeId;
    }

    @Override
    public boolean isNew() {
        return nueva;
    }

    @PostLoad
    @PostPersist
    void marcarGuardada() {
        this.nueva = false;
    }
}