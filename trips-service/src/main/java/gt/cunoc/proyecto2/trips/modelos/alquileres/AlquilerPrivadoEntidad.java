package gt.cunoc.proyecto2.trips.modelos.alquileres;

import gt.cunoc.proyecto2.trips.errores.alquileres.ErrorAlquiler;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tabla alquileres_privados (V11). Sin setters: cada cambio de estado es un método que valida
 * desde qué estado se puede llegar. fecha_creacion y fecha_actualizacion las maneja la BD.
 */
@Entity(name = "AlquilerPrivado")
@Table(name = "alquileres_privados")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AlquilerPrivadoEntidad {

    @Id
    private UUID id;

    @Column(name = "sucursal_id", nullable = false, updatable = false)
    private UUID sucursalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private CanalAlquiler canal;

    @Column(name = "cliente_usuario_id", updatable = false)
    private UUID clienteUsuarioId;

    @Column(name = "cliente_nit", nullable = false, length = 20, updatable = false)
    private String clienteNit;

    @Column(name = "cliente_nombre", nullable = false, length = 150, updatable = false)
    private String clienteNombre;

    @Column(name = "cliente_correo", length = 150, updatable = false)
    private String clienteCorreo;

    @Column(nullable = false, updatable = false)
    private String origen;

    @Column(nullable = false, updatable = false)
    private String destino;

    @Column(name = "origen_latitud", precision = 9, scale = 6, updatable = false)
    private BigDecimal origenLatitud;

    @Column(name = "origen_longitud", precision = 9, scale = 6, updatable = false)
    private BigDecimal origenLongitud;

    @Column(name = "destino_latitud", precision = 9, scale = 6, updatable = false)
    private BigDecimal destinoLatitud;

    @Column(name = "destino_longitud", precision = 9, scale = 6, updatable = false)
    private BigDecimal destinoLongitud;

    @Column(name = "fecha_salida", nullable = false, updatable = false)
    private OffsetDateTime fechaSalida;

    @Column(name = "fecha_retorno", updatable = false)
    private OffsetDateTime fechaRetorno;

    @Column(name = "cantidad_pasajeros", nullable = false, updatable = false)
    private int cantidadPasajeros;

    @Column(name = "km_estimados", nullable = false, precision = 8, scale = 2, updatable = false)
    private BigDecimal kmEstimados;

    @Column(name = "precio_estimado", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal precioEstimado;

    @Column(name = "precio_confirmado", precision = 12, scale = 2)
    private BigDecimal precioConfirmado;

    @Column(name = "confirmado_por")
    private UUID confirmadoPor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private EstadoAlquiler estado;

    @Column(name = "motivo_cancelacion")
    private String motivoCancelacion;

    /**
     * V16: momento exacto de la cancelación, el que indicó quien canceló. Es lo que fija el tramo de
     * la tarifa de reembolso, por eso lo manda el frontend igual que la fecha de solicitud en T16.
     */
    @Column(name = "fecha_hora_cancelacion")
    private OffsetDateTime fechaHoraCancelacion;

    @Column(name = "fecha_solicitud", nullable = false, updatable = false)
    private LocalDate fechaSolicitud;

    @Column(name = "solicitado_por", nullable = false, updatable = false)
    private UUID solicitadoPor;

    /**
     * La maneja la BD (trigger de V11): acá solo se lee, para ordenar los listados por lo último
     * que se pidió o se tocó. insertable/updatable = false para no pisar al trigger.
     */
    @Column(name = "fecha_actualizacion", insertable = false, updatable = false)
    private OffsetDateTime fechaActualizacion;

    /** Null al crear (así Spring Data sabe que es nuevo); Hibernate pone 0 y la sube en cada UPDATE. */
    @Version
    private Integer version;

    public static AlquilerPrivadoEntidad solicitar(UUID sucursalId, CanalAlquiler canal, ClienteAlquiler cliente,
                                                   RecorridoAlquiler recorrido, BigDecimal precioEstimado,
                                                   LocalDate fechaSolicitud, UUID solicitadoPor) {
        AlquilerPrivadoEntidad alquiler = new AlquilerPrivadoEntidad();
        alquiler.id = UUID.randomUUID();
        alquiler.sucursalId = sucursalId;
        alquiler.canal = canal;
        alquiler.clienteUsuarioId = cliente.usuarioId();
        alquiler.clienteNit = cliente.nit();
        alquiler.clienteNombre = cliente.nombre();
        alquiler.clienteCorreo = cliente.correo();
        alquiler.origen = recorrido.origen();
        alquiler.destino = recorrido.destino();
        alquiler.origenLatitud = recorrido.origenLatitud();
        alquiler.origenLongitud = recorrido.origenLongitud();
        alquiler.destinoLatitud = recorrido.destinoLatitud();
        alquiler.destinoLongitud = recorrido.destinoLongitud();
        alquiler.fechaSalida = recorrido.fechaSalida();
        alquiler.fechaRetorno = recorrido.fechaRetorno();
        alquiler.cantidadPasajeros = recorrido.cantidadPasajeros();
        alquiler.kmEstimados = recorrido.kmEstimados();
        alquiler.precioEstimado = precioEstimado;
        alquiler.estado = EstadoAlquiler.SOLICITADO;
        alquiler.fechaSolicitud = fechaSolicitud;
        alquiler.solicitadoPor = solicitadoPor;
        return alquiler;
    }

    public void cotizar(BigDecimal precioConfirmado, UUID confirmadoPor) {
        verificarEstado(EstadoAlquiler.SOLICITADO);
        this.precioConfirmado = precioConfirmado;
        this.confirmadoPor = confirmadoPor;
        this.estado = EstadoAlquiler.COTIZADO;
    }

    /**
     * T21. Se cancela desde cualquiera de los cuatro estados previos a COMPLETADO: antes de pagar no
     * hay dinero en juego, y después de pagar la tarifa de reembolso (Sales) decide si la cancelación
     * se permite a esa hora y qué porcentaje se devuelve. Esa validación va antes de llamar a este
     * método; acá solo se vigila que el alquiler no esté ya completado ni cancelado.
     *
     * @param cuando momento de la cancelación que indicó el usuario; es el que fija el tramo de la
     *               tarifa y el que viaja en AlquilerCancelado para que Sales calcule el reembolso
     */
    public void cancelar(String motivo, OffsetDateTime cuando) {
        if (!EstadoAlquiler.CANCELABLES.contains(estado)) {
            throw ErrorAlquiler.ESTADO_NO_CANCELABLE.excepcion(estado);
        }
        this.motivoCancelacion = motivo;
        this.fechaHoraCancelacion = cuando;
        this.estado = EstadoAlquiler.CANCELADO;
    }

    /** Desde PAGADO ya entró dinero: cancelar obliga a devolver el porcentaje que toque. */
    public boolean yaSePago() {
        return estado.conPagoHecho();
    }

    public boolean marcarPagado() {
        if (estado != EstadoAlquiler.COTIZADO) {
            return false;
        }
        this.estado = EstadoAlquiler.PAGADO;
        return true;
    }

    public void asignar() {
        verificarPagado();
        this.estado = EstadoAlquiler.ASIGNADO;
    }

    public void verificarPagado() {
        if (estado != EstadoAlquiler.PAGADO) {
            throw ErrorAlquiler.ALQUILER_NO_PAGADO.excepcion(estado);
        }
    }

    public void liberar() {
        if (estado == EstadoAlquiler.ASIGNADO) {
            this.estado = EstadoAlquiler.PAGADO;
        }
    }

    public void completar() {
        if (estado == EstadoAlquiler.ASIGNADO) {
            this.estado = EstadoAlquiler.COMPLETADO;
        }
    }

    public boolean esDelCliente(UUID usuarioId) {
        return clienteUsuarioId != null && clienteUsuarioId.equals(usuarioId);
    }

    private void verificarEstado(EstadoAlquiler requerido) {
        if (estado != requerido) {
            throw ErrorAlquiler.ESTADO_INVALIDO.excepcion(estado, requerido);
        }
    }
}