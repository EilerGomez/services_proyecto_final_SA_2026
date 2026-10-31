package gt.cunoc.proyecto2.trips.modelos.viajes;

import gt.cunoc.proyecto2.trips.errores.operaciones.ErrorOperacionViaje;
import gt.cunoc.proyecto2.trips.errores.viajes.ErrorViaje;
import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
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

    /** El chofer de un viaje PRIVADO gana 15 % más (enunciado). */
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

    /**
     * V17. Hacia dónde va dentro de su ruta. Un REGRESO comparte la ruta con su ida y solo se lee al
     * revés; no cambia después de programarse, porque cambiarlo invertiría el recorrido de boletos
     * ya vendidos.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8, updatable = false)
    private SentidoViaje sentido;

    /** V17. El piloto asignado puede vender boletos presenciales de este viaje. */
    @Column(name = "venta_por_chofer", nullable = false)
    private boolean ventaPorChofer;

    /**
     * V18. Solo en un REGRESO: el viaje de ida que le dio origen. Con él se exige que la vuelta no
     * salga antes de que la ida llegue, y que las dos vayan en el mismo bus. Queda en null si la ida
     * se elimina (ON DELETE SET NULL) o si el regreso se programó antes de la V18.
     */
    @Column(name = "viaje_ida_id", updatable = false)
    private UUID viajeIdaId;

    /**
     * La maneja la BD (trigger de V7): acá solo se lee, para poder ordenar los listados por lo
     * último que se programó o se tocó. insertable/updatable = false para no pisar al trigger.
     */
    @Column(name = "fecha_actualizacion", insertable = false, updatable = false)
    private OffsetDateTime fechaActualizacion;

    @Column(name = "creado_por", nullable = false, updatable = false)
    private UUID creadoPor;

    @Version
    private Integer version;

    /**
     * T10. Viaje regular sobre una ruta.
     *
     * @param sentido         IDA, o REGRESO para el viaje de vuelta por la misma ruta
     * @param ventaPorChofer  si el piloto asignado puede vender boletos presenciales de este viaje
     */
    public static ViajeEntidad programarRegular(RutaEntidad ruta, AsignacionViaje asignacion, UUID creadoPor,
                                                SentidoViaje sentido, boolean ventaPorChofer) {
        ViajeEntidad viaje = new ViajeEntidad();
        viaje.id = UUID.randomUUID();
        viaje.tipo = TipoViaje.REGULAR;
        viaje.rutaId = ruta.getId();
        // La sucursal que opera el viaje es siempre la de origen de la ruta, vaya o venga: es la que
        // lo programó y la que responde por él en los listados y en los reportes.
        viaje.sucursalId = ruta.getSucursalOrigenId();
        viaje.precioBoleto = ruta.getPrecioBoleto();
        viaje.estado = EstadoViaje.PROGRAMADO;
        viaje.sentido = sentido == null ? SentidoViaje.IDA : sentido;
        viaje.ventaPorChofer = ventaPorChofer;
        viaje.creadoPor = creadoPor;
        viaje.aplicar(asignacion);
        return viaje;
    }

    /**
     * T10. El viaje de vuelta de una ida recién programada: misma ruta al revés, el mismo bus y la
     * ida apuntada para poder exigir que llegue antes de que este salga.
     */
    public static ViajeEntidad programarRegreso(RutaEntidad ruta, AsignacionViaje asignacion, UUID creadoPor,
                                                boolean ventaPorChofer, ViajeEntidad ida) {
        if (!ida.getBusId().equals(asignacion.busId())) {
            throw ErrorViaje.REGRESO_CON_OTRO_BUS.excepcion();
        }
        ViajeEntidad vuelta = programarRegular(ruta, asignacion, creadoPor, SentidoViaje.REGRESO, ventaPorChofer);
        vuelta.viajeIdaId = ida.getId();
        return vuelta;
    }

    public static ViajeEntidad programarPrivado(AlquilerPrivadoEntidad alquiler, AsignacionViaje asignacion,
                                                UUID creadoPor) {
        ViajeEntidad viaje = new ViajeEntidad();
        viaje.id = UUID.randomUUID();
        viaje.tipo = TipoViaje.PRIVADO;
        viaje.alquilerId = alquiler.getId();
        viaje.sucursalId = alquiler.getSucursalId();
        viaje.estado = EstadoViaje.PROGRAMADO;
        // Un privado nace de un alquiler y no tiene vuelta propia: su retorno va en el alquiler.
        viaje.sentido = SentidoViaje.IDA;
        viaje.ventaPorChofer = false;
        viaje.creadoPor = creadoPor;
        viaje.aplicar(asignacion);
        return viaje;
    }

    /*
     * Cambia bus, chofer u horario de un viaje PROGRAMADO.
     * El bus nuevo no puede tener menos asientos: Sales ya pudo vender hasta la capacidad actual.
     */
    public void reprogramar(AsignacionViaje asignacion) {
        verificarProgramado();
        if (asignacion.capacidadBus() < capacidadBus) {
            throw ErrorViaje.CAPACIDAD_MENOR.excepcion(asignacion.capacidadBus(), capacidadBus);
        }
        aplicar(asignacion);
    }

    /*VIAJE_YA_INICIO si ya salió (T11, T12). */
    public void verificarProgramado() {
        if (estado != EstadoViaje.PROGRAMADO) {
            throw ErrorViaje.VIAJE_YA_INICIO.excepcion();
        }
    }

    /* PROGRAMADO → EN_RUTA. 409 SALIDA_YA_REGISTRADA si ya salió. */
    public void iniciar() {
        if (estado != EstadoViaje.PROGRAMADO) {
            throw ErrorOperacionViaje.SALIDA_YA_REGISTRADA.excepcion();
        }
        this.estado = EstadoViaje.EN_RUTA;
    }

    /* EN_RUTA → COMPLETADO. 422 VIAJE_NO_INICIADO o 409 LLEGADA_YA_REGISTRADA. */
    public void completar() {
        if (estado == EstadoViaje.PROGRAMADO) {
            throw ErrorOperacionViaje.VIAJE_NO_INICIADO.excepcion();
        }
        if (estado == EstadoViaje.COMPLETADO) {
            throw ErrorOperacionViaje.LLEGADA_YA_REGISTRADA.excepcion();
        }
        this.estado = EstadoViaje.COMPLETADO;
    }

    /* Lo que gana el chofer por este viaje: el salario base copiado, × 1.15 si es PRIVADO. */
    public BigDecimal salarioChoferPorViaje() {
        if (tipo == TipoViaje.PRIVADO) {
            return salarioBaseChofer.multiply(RECARGO_PRIVADO).setScale(2, RoundingMode.HALF_UP);
        }
        return salarioBaseChofer;
    }

    /* por qué no se pueden vender boletos (null = sí se puede). */
    public MotivoNoVendible motivoNoVendible() {
        if (tipo == TipoViaje.PRIVADO) {
            return MotivoNoVendible.VIAJE_PRIVADO;
        }
        if (estado != EstadoViaje.PROGRAMADO) {
            return MotivoNoVendible.VIAJE_YA_INICIO;
        }
        return null;
    }

    /** El viaje de vuelta de su ruta: el recorrido se lee al revés (destino → origen). */
    public boolean esRegreso() {
        return sentido != null && sentido.esRegreso();
    }

    /**
     * T11. Al reprogramar un regreso el bus no se puede cambiar por otro: es el mismo bus que se fue
     * de ida el que se regresa. El chofer y el horario sí se pueden cambiar.
     */
    public void verificarMismoBusQueLaIda(UUID busDeLaIda) {
        if (busDeLaIda != null && !busDeLaIda.equals(busId)) {
            throw ErrorViaje.REGRESO_CON_OTRO_BUS.excepcion();
        }
    }

    /**
     * T11. Habilita o quita la venta de boletos por el piloto. Solo mientras el viaje no haya salido:
     * después ya nadie vende, ni el piloto ni la ventanilla.
     */
    public void permitirVentaPorChofer(boolean permitida) {
        verificarProgramado();
        this.ventaPorChofer = permitida;
    }

    /**
     * ¿Este piloto puede vender boletos de este viaje? Lo pregunta Sales por el endpoint interno
     * antes de dejarlo vender: tiene que estar habilitada la venta, ser el chofer asignado y no
     * haber salido todavía.
     */
    public boolean puedeVenderElChofer(UUID choferConsultado) {
        return ventaPorChofer
                && esRegular()
                && estado == EstadoViaje.PROGRAMADO
                && this.choferId.equals(choferConsultado);
    }

    public boolean esRegular() {
        return tipo == TipoViaje.REGULAR;
    }

    public boolean esPrivado() {
        return tipo == TipoViaje.PRIVADO;
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