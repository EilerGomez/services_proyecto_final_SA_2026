package gt.cunoc.proyecto2.fleet.modelos.buses;

import gt.cunoc.proyecto2.fleet.errores.buses.ErrorBus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Tabla buses (V4).
 * Solo los datos editables en F13 tienen setter; el estado cambia con cambiarEstado(...)
 * y el kilometraje con los eventos de viaje (paso de eventos).
 */
@Entity(name = "Bus")
@Table(name = "buses")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BusEntidad {

    @Id
    private UUID id;

    @Column(name = "sucursal_id", nullable = false, updatable = false)
    private UUID sucursalId;

    @Column(nullable = false, unique = true, length = 10, updatable = false)
    private String placa;

    @Setter
    @Column(nullable = false, length = 50)
    private String marca;

    @Setter
    @Column(nullable = false, length = 50)
    private String modelo;

    @Setter
    @Column(name = "anio_fabricacion", nullable = false)
    private int anioFabricacion;

    @Setter
    @Column(name = "capacidad_pasajeros", nullable = false)
    private int capacidadPasajeros;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoBus estado;

    @Column(name = "kilometraje_actual", nullable = false, precision = 12, scale = 2)
    private BigDecimal kilometrajeActual;

    @Setter
    @Column(name = "foto_url", nullable = false, length = 500)
    private String fotoUrl;

    @Version
    private Integer version;

    /** Bus nuevo, siempre ACTIVO. */
    public static BusEntidad registrar(UUID sucursalId, String placa, String marca, String modelo,
                                       int anioFabricacion, int capacidadPasajeros,
                                       BigDecimal kilometrajeInicial, String fotoUrl) {
        BusEntidad bus = new BusEntidad();
        bus.id = UUID.randomUUID();
        bus.sucursalId = sucursalId;
        bus.placa = placa;
        bus.marca = marca;
        bus.modelo = modelo;
        bus.anioFabricacion = anioFabricacion;
        bus.capacidadPasajeros = capacidadPasajeros;
        bus.estado = EstadoBus.ACTIVO;
        bus.kilometrajeActual = kilometrajeInicial;
        bus.fotoUrl = fotoUrl;
        return bus;
    }

    /** Reglas del cambio manual de estado (F14) que no necesitan otros servicios. */
    public void validarCambioDeEstado(EstadoBus nuevo) {
        if (!nuevo.seAsignaManualmente()) {
            throw ErrorBus.ESTADO_NO_PERMITIDO.excepcion(nuevo);
        }
        if (estado == EstadoBus.EN_RUTA) {
            throw ErrorBus.BUS_EN_RUTA.excepcion();
        }
    }

    /**
     * F14. Cambio manual de estado.
     * La regla "INACTIVO solo sin viajes" necesita R3 y la valida el servicio antes de llamar aquí.
     */
    public void cambiarEstado(EstadoBus nuevo) {
        validarCambioDeEstado(nuevo);
        this.estado = nuevo;
    }
}