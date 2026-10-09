package gt.cunoc.proyecto2.trips.modelos.rutas;

import gt.cunoc.proyecto2.trips.errores.rutas.ErrorRuta;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "Ruta")
@Table(name = "rutas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RutaEntidad {

    @Id
    private UUID id;

    @Column(name = "sucursal_origen_id", nullable = false, updatable = false)
    private UUID sucursalOrigenId;

    @Column(name = "sucursal_destino_id", nullable = false, updatable = false)
    private UUID sucursalDestinoId;

    @Column(name = "distancia_km", nullable = false, precision = 8, scale = 2)
    private BigDecimal distanciaKm;

    @Column(name = "precio_boleto", nullable = false, precision = 12, scale = 2)
    private BigDecimal precioBoleto;

    @Column(nullable = false)
    private boolean activa;

    @Column(name = "creado_por", nullable = false, updatable = false)
    private UUID creadoPor;

    /** Null al crear (así Spring Data sabe que es nueva); Hibernate pone 0 y la sube en cada UPDATE. */
    @Version
    private Integer version;

    /** Ruta nueva, activa. */
    public static RutaEntidad registrar(UUID sucursalOrigenId, UUID sucursalDestinoId, BigDecimal distanciaKm,
                                        BigDecimal precioBoleto, UUID creadoPor) {
        if (sucursalOrigenId.equals(sucursalDestinoId)) {
            throw ErrorRuta.MISMA_SUCURSAL.excepcion();
        }
        RutaEntidad ruta = new RutaEntidad();
        ruta.id = UUID.randomUUID();
        ruta.sucursalOrigenId = sucursalOrigenId;
        ruta.sucursalDestinoId = sucursalDestinoId;
        ruta.distanciaKm = distanciaKm;
        ruta.precioBoleto = precioBoleto;
        ruta.activa = true;
        ruta.creadoPor = creadoPor;
        return ruta;
    }

    /** T6. El precio nuevo aplica a los viajes que se programen después (cada viaje guarda su copia). */
    public void actualizar(BigDecimal distanciaKm, BigDecimal precioBoleto, boolean activa) {
        this.distanciaKm = distanciaKm;
        this.precioBoleto = precioBoleto;
        this.activa = activa;
    }

    /** true si el cambio la pasa de inactiva a activa (hay que revisar que las sucursales sigan activas). */
    public boolean seReactiva(boolean nuevaActiva) {
        return !activa && nuevaActiva;
    }
}