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

    @Column(name = "fecha_creacion", insertable = false, updatable = false)
    private OffsetDateTime fechaCreacion;

    @Version
    private Integer version;

    public static OrdenAlquilerEntidad registrar(UUID alquilerId, UUID sucursalId, ClienteVenta cliente,
                                                 String origen, String destino, BigDecimal monto) {
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
