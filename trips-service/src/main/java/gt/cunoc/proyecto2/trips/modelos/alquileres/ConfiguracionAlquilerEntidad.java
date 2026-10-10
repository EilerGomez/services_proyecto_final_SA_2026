package gt.cunoc.proyecto2.trips.modelos.alquileres;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Tabla configuracion_alquiler (V2): historial del precio por km del alquiler privado.
 * Inmutable: un precio nuevo es otra fila con otra fecha (el trigger también impide UPDATE y DELETE).
 * fecha_creacion la pone la BD, por eso no se mapea.
 */
@Entity(name = "ConfiguracionAlquiler")
@Table(name = "configuracion_alquiler")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConfiguracionAlquilerEntidad {

    /** Lo genera Hibernate al guardar: así Spring Data sabe que es nueva y hace INSERT directo. */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "precio_por_km", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioPorKm;

    @Column(name = "vigente_desde", nullable = false, unique = true)
    private LocalDate vigenteDesde;

    @Column(name = "creado_por", nullable = false)
    private UUID creadoPor;

    public static ConfiguracionAlquilerEntidad registrar(BigDecimal precioPorKm, LocalDate vigenteDesde,
                                                         UUID creadoPor) {
        ConfiguracionAlquilerEntidad configuracion = new ConfiguracionAlquilerEntidad();
        configuracion.precioPorKm = precioPorKm;
        configuracion.vigenteDesde = vigenteDesde;
        configuracion.creadoPor = creadoPor;
        return configuracion;
    }

    /** T16: precio estimado de un alquiler = km × precio por km, a 2 decimales. */
    public BigDecimal precioPara(BigDecimal km) {
        return km.multiply(precioPorKm).setScale(2, RoundingMode.HALF_UP);
    }
}