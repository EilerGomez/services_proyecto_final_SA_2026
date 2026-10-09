package gt.cunoc.proyecto2.trips.modelos.viajes;

import gt.cunoc.proyecto2.trips.errores.viajes.ErrorViaje;
import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tabla viajes (V7). Sin setters: se programa con programarRegular(...), se cambia con reprogramar(...).
 * tipo, ruta, alquiler, sucursal y precio del boleto no cambian (updatable = false).
 * fecha_creacion y fecha_actualizacion las maneja la BD.
 */
@Entity(name = "Viaje")
@Table(name = "viajes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ViajeEntidad {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private TipoViaje tipo;

    @Column(name = "ruta_id", updatable = false)
    private UUID rutaId;

    @Column(name = "alquiler_id", updatable = false)
    private UUID alquilerId;

    @Column(name = "sucursal_id", nullable = false, updatable = false)
    private UUID sucursalId;

    @Column(name = "bus_id", nullable = false)
    private UUID busId;

    @Column(name = "chofer_id", nullable = false)
    private UUID choferId;

    @Column(name = "capacidad_bus", nullable = false)
    private int capacidadBus;

    @Column(name = "precio_boleto", precision = 12, scale = 2, updatable = false)
    private BigDecimal precioBoleto;

    @Column(name = "salario_base_chofer", nullable = false, precision = 12, scale = 2)
    private BigDecimal salarioBaseChofer;

    @Column(name = "fecha_salida_programada", nullable = false)
    private OffsetDateTime fechaSalidaProgramada;

    @Column(name = "fecha_llegada_estimada", nullable = false)
    private OffsetDateTime fechaLlegadaEstimada;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoViaje estado;

    @Column(name = "creado_por", nullable = false, updatable = false)
    private UUID creadoPor;

    /** Null al crear (así Spring Data sabe que es nuevo); Hibernate pone 0 y la sube en cada UPDATE. */
    @Version
    private Integer version;

    /** T10. Viaje REGULAR de una ruta: copia el precio del boleto de la ruta en este momento. */
    public static ViajeEntidad programarRegular(RutaEntidad ruta, AsignacionViaje asignacion, UUID creadoPor) {
        ViajeEntidad viaje = new ViajeEntidad();
        viaje.id = UUID.randomUUID();
        viaje.tipo = TipoViaje.REGULAR;
        viaje.rutaId = ruta.getId();
        viaje.sucursalId = ruta.getSucursalOrigenId();
        viaje.precioBoleto = ruta.getPrecioBoleto();
        viaje.estado = EstadoViaje.PROGRAMADO;
        viaje.creadoPor = creadoPor;
        viaje.aplicar(asignacion);
        return viaje;
    }


    public void reprogramar(AsignacionViaje asignacion) {
        verificarProgramado();
        if (asignacion.capacidadBus() < capacidadBus) {
            throw ErrorViaje.CAPACIDAD_MENOR.excepcion(asignacion.capacidadBus(), capacidadBus);
        }
        aplicar(asignacion);
    }

    public void verificarProgramado() {
        if (estado != EstadoViaje.PROGRAMADO) {
            throw ErrorViaje.VIAJE_YA_INICIO.excepcion();
        }
    }

    public MotivoNoVendible motivoNoVendible() {
        if (tipo == TipoViaje.PRIVADO) {
            return MotivoNoVendible.VIAJE_PRIVADO;
        }
        if (estado != EstadoViaje.PROGRAMADO) {
            return MotivoNoVendible.VIAJE_YA_INICIO;
        }
        return null;
    }

    public boolean esRegular() {
        return tipo == TipoViaje.REGULAR;
    }

    private void aplicar(AsignacionViaje asignacion) {
        this.busId = asignacion.busId();
        this.capacidadBus = asignacion.capacidadBus();
        this.choferId = asignacion.choferId();
        this.salarioBaseChofer = asignacion.salarioBaseChofer();
        this.fechaSalidaProgramada = asignacion.fechaSalidaProgramada();
        this.fechaLlegadaEstimada = asignacion.fechaLlegadaEstimada();
    }
}