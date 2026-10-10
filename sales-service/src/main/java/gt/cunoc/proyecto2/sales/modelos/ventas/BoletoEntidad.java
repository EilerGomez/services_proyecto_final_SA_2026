package gt.cunoc.proyecto2.sales.modelos.ventas;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "Boleto")
@Table(name = "boletos")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoletoEntidad {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "venta_id", nullable = false, updatable = false)
    private UUID ventaId;

    @Column(name = "viaje_id", nullable = false, updatable = false)
    private UUID viajeId;

    @Column(name = "numero_asiento", nullable = false, updatable = false)
    private int numeroAsiento;

    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal precio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoBoleto estado;

    public static BoletoEntidad emitir(VentaEntidad venta, int numeroAsiento, BigDecimal precio) {
        BoletoEntidad boleto = new BoletoEntidad();
        boleto.ventaId = venta.getId();
        boleto.viajeId = venta.getViajeId();
        boleto.numeroAsiento = numeroAsiento;
        boleto.precio = precio;
        boleto.estado = EstadoBoleto.ACTIVO;
        return boleto;
    }

    public void cancelar() {
        this.estado = EstadoBoleto.CANCELADO;
    }
}