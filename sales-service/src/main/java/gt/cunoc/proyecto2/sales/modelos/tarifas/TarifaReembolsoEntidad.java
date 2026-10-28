package gt.cunoc.proyecto2.sales.modelos.tarifas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tabla tarifas_reembolso (V13): cuánto se le devuelve al cliente si cancela su alquiler, según el
 * estado del alquiler y las horas que faltaban para la salida. La administra el ADMIN_SISTEMA.
 *
 * <p>Sin setters: {@link #editar} valida lo mismo que los CHECK de la tabla, y desactivar es su
 * propio método para que una tarifa nunca se borre (hay órdenes viejas que la citan).</p>
 */
@Entity(name = "TarifaReembolso")
@Table(name = "tarifas_reembolso")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TarifaReembolsoEntidad {

    public static final BigDecimal CIEN = new BigDecimal("100");

    /** Lo genera Hibernate al guardar: así Spring Data sabe que es nueva y hace INSERT directo. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_alquiler", nullable = false, length = 12)
    private EstadoAlquilerTarifa estadoAlquiler;

    /** Horas mínimas antes de la salida para que esta tarifa aplique (0 = tramo de último momento). */
    @Column(name = "horas_minimas_antes", nullable = false)
    private int horasMinimasAntes;

    @Column(name = "permite_cancelacion", nullable = false)
    private boolean permiteCancelacion;

    @Column(name = "porcentaje_reembolso", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeReembolso;

    @Column(nullable = false, length = 255)
    private String justificacion;

    /** Una tarifa no se borra: se desactiva, para que las órdenes que la citan sigan explicándose. */
    @Column(nullable = false)
    private boolean activa;

    /** La maneja el trigger de V13: acá solo se lee. */
    @Column(name = "fecha_actualizacion", insertable = false, updatable = false)
    private OffsetDateTime fechaActualizacion;

    @Version
    private Integer version;

    public static TarifaReembolsoEntidad crear(EstadoAlquilerTarifa estado, int horasMinimasAntes,
                                               boolean permiteCancelacion, BigDecimal porcentaje,
                                               String justificacion) {
        TarifaReembolsoEntidad tarifa = new TarifaReembolsoEntidad();
        tarifa.estadoAlquiler = estado;
        tarifa.activa = true;
        tarifa.aplicar(horasMinimasAntes, permiteCancelacion, porcentaje, justificacion);
        return tarifa;
    }

    /** El estado y el tramo de horas identifican la tarifa (UNIQUE): lo demás sí se edita. */
    public void editar(boolean permiteCancelacion, BigDecimal porcentaje, String justificacion) {
        aplicar(this.horasMinimasAntes, permiteCancelacion, porcentaje, justificacion);
    }

    public void desactivar() {
        this.activa = false;
    }

    public void reactivar() {
        this.activa = true;
    }

    /** ¿Esta tarifa cubre una cancelación a la que le faltaban estas horas para la salida? */
    public boolean cubre(EstadoAlquilerTarifa estado, long horasAntes) {
        return activa
                && (estadoAlquiler == estado || estadoAlquiler.esComodin())
                && horasMinimasAntes <= horasAntes;
    }

    /** Lo que se le devuelve de un monto cobrado, a 2 decimales (la columna es NUMERIC(12,2)). */
    public BigDecimal reembolsoDe(BigDecimal montoPagado) {
        return montoPagado.multiply(porcentajeReembolso)
                .divide(CIEN, 2, RoundingMode.HALF_UP);
    }

    private void aplicar(int horasMinimasAntes, boolean permiteCancelacion, BigDecimal porcentaje,
                         String justificacion) {
        this.horasMinimasAntes = horasMinimasAntes;
        this.permiteCancelacion = permiteCancelacion;
        // El CHECK de la tabla dice lo mismo: si no se permite cancelar no hay nada que devolver
        this.porcentajeReembolso = permiteCancelacion ? porcentaje : BigDecimal.ZERO.setScale(2);
        this.justificacion = justificacion;
    }
}
