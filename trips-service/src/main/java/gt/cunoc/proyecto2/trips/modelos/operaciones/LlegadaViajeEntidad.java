package gt.cunoc.proyecto2.trips.modelos.operaciones;

import gt.cunoc.proyecto2.trips.errores.operaciones.ErrorOperacionViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import gt.cunoc.proyecto2.trips.utilidades.Fechas;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.springframework.data.domain.Persistable;

/**
 * Tabla llegadas_viaje (V10): la llegada de un viaje (T15) con sus costos ya calculados.
 * Inmutable (también por trigger). Mismo esquema de id que SalidaViajeEntidad.
 */
@Entity(name = "LlegadaViaje")
@Table(name = "llegadas_viaje")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LlegadaViajeEntidad implements Persistable<UUID> {

    @Id
    @Column(name = "viaje_id")
    private UUID viajeId;

    @Column(name = "fecha_hora_llegada", nullable = false)
    private OffsetDateTime fechaHoraLlegada;

    @Column(name = "km_final", nullable = false, precision = 12, scale = 2)
    private BigDecimal kmFinal;

    @Column(name = "km_recorridos", nullable = false, precision = 12, scale = 2)
    private BigDecimal kmRecorridos;

    @Column(name = "costo_combustible", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoCombustible;

    @Column(name = "tarifa_depreciacion", nullable = false, precision = 12, scale = 4)
    private BigDecimal tarifaDepreciacion;

    @Column(name = "costo_depreciacion", nullable = false, precision = 12, scale = 2)
    private BigDecimal costoDepreciacion;

    @Column(name = "salario_chofer", nullable = false, precision = 12, scale = 2)
    private BigDecimal salarioChofer;

    @Column(name = "registrado_por", nullable = false)
    private UUID registradoPor;

    @Transient
    @Getter(AccessLevel.NONE)
    private boolean nueva;

    public static LlegadaViajeEntidad registrar(ViajeEntidad viaje, SalidaViajeEntidad salida,
                                                OffsetDateTime fechaHoraLlegada, BigDecimal kmFinal,
                                                BigDecimal costoCombustible, BigDecimal tarifaDepreciacion,
                                                UUID registradoPor) {
        if (!fechaHoraLlegada.isAfter(salida.getFechaHoraSalida())) {
            throw ErrorOperacionViaje.LLEGADA_ANTES_DE_SALIDA.excepcion(
                    Fechas.enGuatemala(salida.getFechaHoraSalida()));
        }
        BigDecimal kmRecorridos = kmFinal.subtract(salida.getKmInicial());
        if (kmRecorridos.signum() < 0) {
            throw ErrorOperacionViaje.KM_INVALIDO.excepcion(kmFinal, salida.getKmInicial());
        }
        LlegadaViajeEntidad llegada = new LlegadaViajeEntidad();
        llegada.viajeId = viaje.getId();
        llegada.fechaHoraLlegada = fechaHoraLlegada;
        llegada.kmFinal = kmFinal;
        llegada.kmRecorridos = kmRecorridos;
        llegada.costoCombustible = costoCombustible;
        llegada.tarifaDepreciacion = tarifaDepreciacion;
        llegada.costoDepreciacion = kmRecorridos.multiply(tarifaDepreciacion).setScale(2, RoundingMode.HALF_UP);
        llegada.salarioChofer = viaje.salarioChoferPorViaje();
        llegada.registradoPor = registradoPor;
        llegada.nueva = true;
        return llegada;
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