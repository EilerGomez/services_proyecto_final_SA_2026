package gt.cunoc.proyecto2.reporting.modelos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Copia local de una ruta, usada por los reportes de rutas y por el mapa.
 *
 * <p>Se alimenta con los eventos de creación, actualización y eliminación de Trips. Guarda los
 * nombres de origen y destino para no tener que resolverlos al generar el reporte.
 */
@Entity(name = "ReporteRuta")
@Table(name = "rutas")
@Getter
@Setter
@NoArgsConstructor
public class RutaEntidad {

    @Id
    private UUID id;

    @Column(name = "sucursal_origen_id", nullable = false)
    private UUID sucursalOrigenId;

    @Column(name = "sucursal_destino_id", nullable = false)
    private UUID sucursalDestinoId;

    @Column(name = "origen_nombre", nullable = false, length = 100)
    private String origenNombre;

    @Column(name = "destino_nombre", nullable = false, length = 100)
    private String destinoNombre;

    @Column(name = "distancia_km", nullable = false, precision = 8, scale = 2)
    private BigDecimal distanciaKm;

    @Column(name = "precio_boleto", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBoleto;

    @Column(nullable = false)
    private boolean activa = true;

    /** Trips no manda aggregateVersion para rutas (gana el último): queda en 0. */
    @Column(nullable = false)
    private int version;
}
