package gt.cunoc.proyecto2.sales.modelos.ventas;

import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity(name = "Venta")
@Table(name = "ventas")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VentaEntidad {

    @Id
    private UUID id;

    @Column(name = "viaje_id", nullable = false, updatable = false)
    private UUID viajeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10, updatable = false)
    private CanalVenta canal;

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

    @Column(name = "cajero_usuario_id", updatable = false)
    private UUID cajeroUsuarioId;

    @Enumerated(EnumType.STRING)
    @Column(name = "metodo_pago", nullable = false, length = 10, updatable = false)
    private MetodoPago metodoPago;

    @Column(name = "monto_total", nullable = false, precision = 12, scale = 2, updatable = false)
    private BigDecimal montoTotal;

    @Column(name = "fecha_pago", nullable = false, updatable = false)
    private LocalDate fechaPago;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private EstadoVenta estado;

    @Column(name = "motivo_rechazo")
    private String motivoRechazo;

    @Column(name = "motivo_cancelacion")
    private String motivoCancelacion;

    @Column(name = "fecha_cancelacion")
    private LocalDate fechaCancelacion;

    /** Null al crear (así Spring Data sabe que es nueva); Hibernate pone 0 y la sube en cada UPDATE. */
    @Version
    private Integer version;

    public static VentaEntidad registrarEnMostrador(VistaViajeEntidad viaje, ClienteVenta cliente,
                                                    int cantidadAsientos, LocalDate fechaPago, UUID cajeroId) {
        VentaEntidad venta = nueva(viaje, cliente, cantidadAsientos, fechaPago);
        venta.canal = CanalVenta.PRESENCIAL;
        venta.metodoPago = MetodoPago.EFECTIVO;
        venta.cajeroUsuarioId = cajeroId;
        venta.estado = EstadoVenta.CONFIRMADA;
        return venta;
    }

    public boolean esDelCliente(UUID usuarioId) {
        return clienteUsuarioId != null && clienteUsuarioId.equals(usuarioId);
    }

    private static VentaEntidad nueva(VistaViajeEntidad viaje, ClienteVenta cliente, int cantidadAsientos,
                                      LocalDate fechaPago) {
        VentaEntidad venta = new VentaEntidad();
        venta.id = UUID.randomUUID();
        venta.viajeId = viaje.getViajeId();
        venta.sucursalId = viaje.getSucursalOrigenId();
        venta.clienteUsuarioId = cliente.usuarioId();
        venta.clienteNit = cliente.nit();
        venta.clienteNombre = cliente.nombre();
        venta.clienteCorreo = cliente.correo();
        venta.montoTotal = viaje.getPrecioBoleto().multiply(BigDecimal.valueOf(cantidadAsientos));
        venta.fechaPago = fechaPago;
        return venta;
    }
}