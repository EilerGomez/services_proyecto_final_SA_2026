package gt.cunoc.proyecto2.fleet.modelos.choferes;

import gt.cunoc.proyecto2.fleet.errores.choferes.ErrorChofer;
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

/**
 * Tabla choferes (V5): datos laborales y licencia.
 * La cuenta y el perfil (foto, correo, teléfono) viven en Identity.
 * Sin setters: los cambios se hacen con los métodos de negocio.
 */
@Entity(name = "Chofer")
@Table(name = "choferes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChoferEntidad {

    @Id
    private UUID id;

    @Column(name = "sucursal_id", nullable = false, updatable = false)
    private UUID sucursalId;

    @Column(name = "usuario_id", nullable = false, unique = true, updatable = false)
    private UUID usuarioId;

    @Column(name = "nombre_completo", nullable = false, length = 201)
    private String nombreCompleto;

    @Column(name = "numero_licencia", nullable = false, unique = true, length = 30)
    private String numeroLicencia;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_licencia", nullable = false, length = 2)
    private TipoLicencia tipoLicencia;

    @Column(name = "fecha_vencimiento_licencia", nullable = false)
    private LocalDate fechaVencimientoLicencia;

    @Column(name = "salario_base_viaje", nullable = false, precision = 12, scale = 2)
    private BigDecimal salarioBaseViaje;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoChofer estado;

    @Version
    private Integer version;

    /** Chofer nuevo, ACTIVO. El id es el solicitudId que se envió a R5. */
    public static ChoferEntidad registrar(UUID id, UUID sucursalId, UUID usuarioId, String nombreCompleto,
                                          String numeroLicencia, TipoLicencia tipoLicencia,
                                          LocalDate fechaVencimientoLicencia, BigDecimal salarioBaseViaje) {
        ChoferEntidad chofer = new ChoferEntidad();
        chofer.id = id;
        chofer.sucursalId = sucursalId;
        chofer.usuarioId = usuarioId;
        chofer.nombreCompleto = nombreCompleto;
        chofer.numeroLicencia = numeroLicencia;
        chofer.tipoLicencia = tipoLicencia;
        chofer.fechaVencimientoLicencia = fechaVencimientoLicencia;
        chofer.salarioBaseViaje = salarioBaseViaje;
        chofer.estado = EstadoChofer.ACTIVO;
        return chofer;
    }

    /** F19. */
    public void actualizarSalario(BigDecimal salarioBaseViaje) {
        verificarActivo();
        this.salarioBaseViaje = salarioBaseViaje;
    }

    /** F20. Renovación o corrección de la licencia. */
    public void actualizarLicencia(String numeroLicencia, TipoLicencia tipoLicencia, LocalDate fechaVencimiento) {
        verificarActivo();
        this.numeroLicencia = numeroLicencia;
        this.tipoLicencia = tipoLicencia;
        this.fechaVencimientoLicencia = fechaVencimiento;
    }

    /** F21. Que no tenga viajes activos (R3) lo valida el servicio antes. */
    public void desactivar() {
        verificarActivo();
        this.estado = EstadoChofer.INACTIVO;
    }

    /** R1b: ¿puede manejar en esa fecha? (la licencia vence al final del día indicado). */
    public boolean licenciaVigenteEn(LocalDate fecha) {
        return !fechaVencimientoLicencia.isBefore(fecha);
    }

    public boolean esActivo() {
        return estado == EstadoChofer.ACTIVO;
    }

    private void verificarActivo() {
        if (!esActivo()) {
            throw ErrorChofer.CHOFER_INACTIVO.excepcion();
        }
    }
}