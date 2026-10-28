package gt.cunoc.proyecto2.sales.modelos.alquileres;

import gt.cunoc.proyecto2.sales.errores.alquileres.ErrorOrdenAlquiler;
import gt.cunoc.proyecto2.sales.modelos.ventas.ClienteVenta;
import gt.cunoc.proyecto2.sales.modelos.ventas.MetodoPago;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "OrdenAlquiler")
@Table(name = "ordenes_alquiler")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrdenAlquilerEntidad {

    @Id
    private UUID id;

    @Column(name = "alquiler_id", nullable = false, updatable = false)
    private UUID alquilerId;

    @Column(name = "cliente_usuario_id", updatable = false)
    private UUID clienteUsuarioId;

    @Column(name = "cliente_nit", nullable = false, length = 20, updatable = false)
    private String clienteNit;

    @Column(name = "cliente_nombre", nullable = false, length = 201, updatable = false)
    private String clienteNombre;

    @Column(name = "cliente_correo", length = 150, updatable = false)
    private String clienteCorreo;

    @Column(name = "sucursal_id", nullable = false, updatable = false)
    private UUID sucursalId;

    @Column(nullable = false, updatable = false)
    private String origen;

    @Column(nullable = false, updatable = false)
    private String destino;

    /** V12: puntos del mapa que marcó el cliente, para dibujar el recorrido al cobrar. */
    @Column(name = "origen_latitud", precision = 9, scale = 6, updatable = false)
    private BigDecimal origenLatitud;

    @Column(name = "origen_longitud", precision = 9, scale = 6, updatable = false)
    private BigDecimal origenLongitud;

    @Column(name = "destino_latitud", precision = 9, scale = 6, updatable = false)
    private BigDecimal destinoLatitud;

    @Column(name = "destino_longitud", precision = 9, scale = 6, updatable = false)
    private BigDecimal destinoLongitud;

    @Column(name = "fecha_salida", updatable = false)
    private OffsetDateTime fechaSalida;

    @Column(name = "fecha_retorno", updatable = false)
    private OffsetDateTime fechaRetorno;

    @Column(nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", length = 10)
    private MetodoPago metodoPago;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EstadoOrdenAlquiler estado;

    @Column(name = "motivo_rechazo")
    private String motivoRechazo;

    // ----- V14: cancelación del alquiler y devolución del dinero -----

    @Column(name = "fecha_cancelacion")
    private LocalDate fechaCancelacion;

    @Column(name = "motivo_cancelacion", length = 500)
    private String motivoCancelacion;

    /** Horas que faltaban para la salida al cancelar: explica qué tramo de la tarifa se aplicó. */
    @Column(name = "horas_antes_viaje")
    private Integer horasAntesViaje;

    @Column(name = "porcentaje_reembolso", precision = 5, scale = 2)
    private BigDecimal porcentajeReembolso;

    @Column(name = "justificacion_reembolso", length = 255)
    private String justificacionReembolso;

    /** Lo que le toca al cliente según la tarifa. 0 cuando no se había pagado o no hay derecho. */
    @Column(name = "monto_reembolsado", precision = 12, scale = 2)
    private BigDecimal montoReembolsado;

    /** Con fecha ya no queda nada por devolver: el dinero volvió o nunca hubo derecho a devolución. */
    @Column(name = "fecha_reembolso")
    private LocalDate fechaReembolso;

    /** Quién entregó el efectivo en la ventanilla; null cuando lo devolvió la cartera. */
    @Column(name = "reembolsado_por")
    private UUID reembolsadoPor;

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Version
    private Integer version;

    public static OrdenAlquilerEntidad registrar(UUID alquilerId, UUID sucursalId, ClienteVenta cliente,
                                                 String origen, String destino, RecorridoOrden recorrido,
                                                 OffsetDateTime fechaSalida, OffsetDateTime fechaRetorno,
                                                 BigDecimal monto) {
        OrdenAlquilerEntidad orden = new OrdenAlquilerEntidad();
        orden.id = UUID.randomUUID();
        orden.alquilerId = alquilerId;
        orden.sucursalId = sucursalId;
        orden.clienteUsuarioId = cliente.usuarioId();
        orden.clienteNit = cliente.nit();
        orden.clienteNombre = cliente.nombre();
        orden.clienteCorreo = cliente.correo();
        orden.origen = origen;
        orden.destino = destino;
        orden.origenLatitud = recorrido.origenLatitud();
        orden.origenLongitud = recorrido.origenLongitud();
        orden.destinoLatitud = recorrido.destinoLatitud();
        orden.destinoLongitud = recorrido.destinoLongitud();
        orden.fechaSalida = fechaSalida;
        orden.fechaRetorno = fechaRetorno;
        orden.monto = monto;
        orden.estado = EstadoOrdenAlquiler.PENDIENTE_PAGO;
        return orden;
    }

    public void solicitarPagoConCartera(LocalDate fecha) {
        if (clienteUsuarioId == null) {
            throw ErrorOrdenAlquiler.ALQUILER_SIN_CUENTA.excepcion();
        }
        verificarPagable();
        if (estado == EstadoOrdenAlquiler.PENDIENTE_PAGO && metodoPago == MetodoPago.CARTERA) {
            throw ErrorOrdenAlquiler.PAGO_EN_PROCESO.excepcion();
        }
        this.metodoPago = MetodoPago.CARTERA;
        this.fechaPago = fecha;
        this.estado = EstadoOrdenAlquiler.PENDIENTE_PAGO;
        this.motivoRechazo = null;
    }

    public void pagarEnEfectivo(LocalDate fecha) {
        verificarPagable();
        this.metodoPago = MetodoPago.EFECTIVO;
        this.fechaPago = fecha;
        this.estado = EstadoOrdenAlquiler.PAGADA;
        this.motivoRechazo = null;
    }

    public void confirmarPagoConCartera() {
        verificarCobroEnCurso();
        this.estado = EstadoOrdenAlquiler.PAGADA;
        this.motivoRechazo = null;
    }

    public void rechazarPagoConCartera(String motivo) {
        verificarCobroEnCurso();
        this.estado = EstadoOrdenAlquiler.RECHAZADA;
        this.motivoRechazo = motivo;
    }

    /**
     * Se canceló el alquiler. Si la orden ya estaba PAGADA se aplica la tarifa de reembolso y queda
     * dinero pendiente de devolver; si no se había pagado, se cierra sin nada que devolver.
     *
     * @param porcentaje     porcentaje de la tarifa que se aplicó (null si no se había pagado)
     * @param justificacion  texto de la tarifa, copiado para que el reembolso se pueda explicar después
     */
    public void cancelar(String motivo, LocalDate fecha, Integer horasAntes, BigDecimal porcentaje,
                         String justificacion) {
        verificarCancelable();
        boolean habiaDinero = estado == EstadoOrdenAlquiler.PAGADA;
        this.motivoCancelacion = motivo;
        this.fechaCancelacion = fecha;
        this.horasAntesViaje = horasAntes;
        this.estado = EstadoOrdenAlquiler.CANCELADA;
        if (!habiaDinero) {
            // Nunca entró dinero: no hay tarifa que aplicar y la orden queda cerrada de una vez
            this.porcentajeReembolso = null;
            this.justificacionReembolso = justificacion;
            this.montoReembolsado = BigDecimal.ZERO.setScale(2);
            this.fechaReembolso = fecha;
            return;
        }
        this.porcentajeReembolso = porcentaje;
        this.justificacionReembolso = justificacion;
        this.montoReembolsado = reembolsoSegun(porcentaje);
        // Sin derecho a devolución no hay nada que entregar: se cierra con el monto en cero
        this.fechaReembolso = this.montoReembolsado.signum() == 0 ? fecha : null;
    }

    /** CarteraReembolsada o S14 del alquiler: CANCELADA → REEMBOLSADA. */
    public void marcarReembolsada(LocalDate fecha, UUID porUsuarioId) {
        verificarPuedeReembolsarse();
        this.fechaReembolso = fechaNoAnteriorAlPago(fecha);
        this.reembolsadoPor = porUsuarioId;
        this.estado = EstadoOrdenAlquiler.REEMBOLSADA;
    }

    /** ¿Queda dinero por entregarle al cliente? Es el filtro de la pantalla de reembolsos. */
    public boolean tieneReembolsoPendiente() {
        return estado == EstadoOrdenAlquiler.CANCELADA && fechaReembolso == null;
    }

    public boolean puedeMarcarseReembolsada() {
        return tieneReembolsoPendiente();
    }

    public boolean estaReembolsada() {
        return estado == EstadoOrdenAlquiler.REEMBOLSADA;
    }

    /**
     * La orden ya no admite otra cancelación: o se canceló, o su dinero ya volvió. Es la guarda de
     * idempotencia del manejador de AlquilerCancelado (un evento reentregado no hace nada).
     */
    public boolean yaCerrada() {
        return estado == EstadoOrdenAlquiler.CANCELADA || estado == EstadoOrdenAlquiler.REEMBOLSADA;
    }

    /** El cobro fue con cartera, aunque la orden ya haya cambiado de estado (se devuelve por evento). */
    public boolean seCobroConCartera() {
        return metodoPago == MetodoPago.CARTERA;
    }

    /**
     * Lo que se le pide a Wallet: el porcentaje de la tarifa cuando la orden se canceló, o el total
     * cuando el reembolso es por un cobro de más (débito tardío, que devuelve todo).
     */
    public BigDecimal montoAReembolsar() {
        return montoReembolsado != null ? montoReembolsado : monto;
    }

    private BigDecimal reembolsoSegun(BigDecimal porcentaje) {
        if (porcentaje == null || porcentaje.signum() <= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return monto.multiply(porcentaje).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private void verificarCancelable() {
        if (estado != EstadoOrdenAlquiler.PENDIENTE_PAGO && estado != EstadoOrdenAlquiler.RECHAZADA
                && estado != EstadoOrdenAlquiler.PAGADA) {
            throw ErrorOrdenAlquiler.ESTADO_INVALIDO_CANCELAR.excepcion(estado);
        }
    }

    private void verificarPuedeReembolsarse() {
        if (estaReembolsada() || fechaReembolso != null) {
            throw ErrorOrdenAlquiler.ORDEN_YA_REEMBOLSADA.excepcion();
        }
        if (!tieneReembolsoPendiente()) {
            throw ErrorOrdenAlquiler.ESTADO_INVALIDO_REEMBOLSO.excepcion(estado);
        }
    }

    public boolean cobroConCarteraEnCurso() {
        return estado == EstadoOrdenAlquiler.PENDIENTE_PAGO && metodoPago == MetodoPago.CARTERA;
    }

    public boolean pagadaConCartera() {
        return estado == EstadoOrdenAlquiler.PAGADA && metodoPago == MetodoPago.CARTERA;
    }

    public LocalDate fechaNoAnteriorAlPago(LocalDate fecha) {
        return fechaPago != null && fecha.isBefore(fechaPago) ? fechaPago : fecha;
    }

    public boolean esDelCliente(UUID usuarioId) {
        return clienteUsuarioId != null && clienteUsuarioId.equals(usuarioId);
    }

    private void verificarCobroEnCurso() {
        if (!cobroConCarteraEnCurso()) {
            throw ErrorOrdenAlquiler.ESTADO_INVALIDO.excepcion(estado);
        }
    }

    private void verificarPagable() {
        if (estado != EstadoOrdenAlquiler.PENDIENTE_PAGO && estado != EstadoOrdenAlquiler.RECHAZADA) {
            throw ErrorOrdenAlquiler.ESTADO_INVALIDO.excepcion(estado);
        }
    }
}
