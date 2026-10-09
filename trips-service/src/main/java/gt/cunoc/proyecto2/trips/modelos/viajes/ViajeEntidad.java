package gt.cunoc.proyecto2.trips.modelos.viajes;

import gt.cunoc.proyecto2.trips.errores.operaciones.ErrorOperacionViaje;
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
import java.math.RoundingMode;
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

    private static final BigDecimal RECARGO_PRIVADO = new BigDecimal("1.15");

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

    @Version
    private Integer version;

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

    /**
     * T11. Cambia bus, chofer u horario de un viaje PROGRAMADO.
     * El bus nuevo no puede tener menos asientos: Sales ya pudo vender hasta la capacidad actual.
     */
    public void reprogramar(AsignacionViaje asignacion) {
        verificarProgramado();
        if (asignacion.capacidadBus() < capacidadBus) {
            throw ErrorViaje.CAPACIDAD_MENOR.excepcion(asignacion.capacidadBus(), capacidadBus);
        }
        aplicar(asignacion);
    }

    /** 422 VIAJE_YA_INICIO si ya salió (T11, T12). */
    public void verificarProgramado() {
        if (estado != EstadoViaje.PROGRAMADO) {
            throw ErrorViaje.VIAJE_YA_INICIO.excepcion();
        }
    }

    /** T14. PROGRAMADO → EN_RUTA. 409 SALIDA_YA_REGISTRADA si ya salió. */
    public void iniciar() {
        if (estado != EstadoViaje.PROGRAMADO) {
            throw ErrorOperacionViaje.SALIDA_YA_REGISTRADA.excepcion();
        }
        this.estado = EstadoViaje.EN_RUTA;
    }

    /** T15. EN_RUTA → COMPLETADO. 422 VIAJE_NO_INICIADO o 409 LLEGADA_YA_REGISTRADA. */
    public void completar() {
        if (estado == EstadoViaje.PROGRAMADO) {
            throw ErrorOperacionViaje.VIAJE_NO_INICIADO.excepcion();
        }
        if (estado == EstadoViaje.COMPLETADO) {
            throw ErrorOperacionViaje.LLEGADA_YA_REGISTRADA.excepcion();
        }
        this.estado = EstadoViaje.COMPLETADO;
    }

    /** Lo que gana el chofer por este viaje: el salario base copiado, × 1.15 si es PRIVADO. */
    public BigDecimal salarioChoferPorViaje() {
        if (tipo == TipoViaje.PRIVADO) {
            return salarioBaseChofer.multiply(RECARGO_PRIVADO).setScale(2, RoundingMode.HALF_UP);
        }
        return salarioBaseChofer;
    }

    /** R2: por qué no se pueden vender boletos (null = sí se puede). */
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