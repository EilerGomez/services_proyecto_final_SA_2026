package gt.cunoc.proyecto2.fleet.modelos.bitacora;

import gt.cunoc.proyecto2.fleet.errores.bitacora.ErrorBitacora;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Tabla bitacora_kilometraje (V6): un registro por viaje completado.
 * Inmutable: Hibernate nunca genera UPDATE para esta entidad.
 */
@Entity(name = "BitacoraKilometraje")
@Table(name = "bitacora_kilometraje")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BitacoraKilometrajeEntidad {

    @Id
    private UUID id;

    @Column(name = "bus_id", nullable = false)
    private UUID busId;

    @Column(name = "viaje_id", nullable = false, unique = true)
    private UUID viajeId;

    @Column(name = "km_inicial", nullable = false, precision = 12, scale = 2)
    private BigDecimal kmInicial;

    @Column(name = "km_final", nullable = false, precision = 12, scale = 2)
    private BigDecimal kmFinal;

    @Column(name = "fecha_llegada", nullable = false)
    private OffsetDateTime fechaLlegada;

    public static BitacoraKilometrajeEntidad registrar(UUID busId, UUID viajeId, BigDecimal kmInicial,
                                                       BigDecimal kmFinal, OffsetDateTime fechaLlegada) {
        if (kmFinal.compareTo(kmInicial) < 0) {
            throw ErrorBitacora.KILOMETRAJE_INVALIDO.excepcion(kmFinal, kmInicial);
        }
        BitacoraKilometrajeEntidad registro = new BitacoraKilometrajeEntidad();
        registro.id = UUID.randomUUID();
        registro.busId = busId;
        registro.viajeId = viajeId;
        registro.kmInicial = kmInicial;
        registro.kmFinal = kmFinal;
        registro.fechaLlegada = fechaLlegada;
        return registro;
    }

    public BigDecimal kmRecorridos() {
        return kmFinal.subtract(kmInicial);
    }
}