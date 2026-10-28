package gt.cunoc.proyecto2.trips.modelos.tarifas;

import gt.cunoc.proyecto2.trips.modelos.alquileres.EstadoAlquiler;
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
 * Tabla vista_tarifas_reembolso (V16): copia local de la política de cancelación de Sales.
 * Solo lectura para JPA: la escribe el manejador de TarifaReembolsoCambiada.
 *
 * <p>Trips la usa para una sola decisión: si la cancelación se permite. El porcentaje que se paga lo
 * calcula Sales con su propia tabla, que es la dueña; acá se guarda solo para poder explicarle al
 * usuario cuánto le tocaría antes de confirmar.</p>
 *
 * <p>estado_alquiler se guarda como texto y no como enum: incluye el comodín CUALQUIERA, que no es un
 * estado de la máquina de estados del alquiler.</p>
 */
@Entity(name = "VistaTarifaReembolso")
@Table(name = "vista_tarifas_reembolso")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VistaTarifaReembolsoEntidad {

    /** Comodín: la tarifa aplica a cualquier estado que no tenga una fila propia para ese tramo. */
    public static final String CUALQUIERA = "CUALQUIERA";

    @Id
    private UUID id;

    @Column(name = "estado_alquiler", nullable = false, length = 12)
    private String estadoAlquiler;

    @Column(name = "horas_minimas_antes", nullable = false)
    private int horasMinimasAntes;

    @Column(name = "permite_cancelacion", nullable = false)
    private boolean permiteCancelacion;

    @Column(name = "porcentaje_reembolso", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeReembolso;

    @Column(nullable = false, length = 255)
    private String justificacion;

    @Column(nullable = false)
    private boolean activa;

    public boolean esComodin() {
        return CUALQUIERA.equals(estadoAlquiler);
    }

    /** ¿Esta tarifa cubre una cancelación de un alquiler en ese estado al que le faltan estas horas? */
    public boolean cubre(EstadoAlquiler estado, long horasAntes) {
        return activa
                && (esComodin() || estadoAlquiler.equals(estado.name()))
                && horasMinimasAntes <= horasAntes;
    }
}
