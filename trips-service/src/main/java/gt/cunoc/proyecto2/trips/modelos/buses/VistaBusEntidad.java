package gt.cunoc.proyecto2.trips.modelos.buses;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

/**
 * Tabla vista_buses (V5): copia local de los buses de Fleet.
 * Solo lectura para JPA: la escriben los manejadores de eventos con SQL (upsert por version).
 */
@Entity(name = "VistaBus")
@Table(name = "vista_buses")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VistaBusEntidad {

    @Id
    private UUID id;

    @Column(name = "sucursal_id", nullable = false)
    private UUID sucursalId;

    @Column(nullable = false, length = 10)
    private String placa;

    @Column(name = "capacidad_pasajeros", nullable = false)
    private int capacidadPasajeros;

    @Column(nullable = false, length = 15)
    private String estado;

    /** V14: foto del bus, para enseñársela al pasajero al comprar. Null si aún no llega su evento. */
    @Column(name = "foto_url", length = 500)
    private String fotoUrl;

    @Column(nullable = false)
    private int version;
}