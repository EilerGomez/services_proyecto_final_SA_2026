/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package gt.cunoc.proyecto2.fleet.modelos.personal;

/**
 *
 * @author eiler
 */
import gt.cunoc.proyecto2.fleet.errores.personal.ErrorPersonal;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Tabla personal_sucursal (V3): admins de sucursal y cajeros.
 * La cuenta (correo, contraseña, perfil) vive en Identity; aquí solo la asignación a una sucursal.
 * Sin setters: los cambios se hacen con los métodos de negocio (asignar, desactivar, trasladarA).
 */
@Entity(name = "PersonalSucursal")
@Table(name = "personal_sucursal")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PersonalSucursalEntidad {

    @Id
    private UUID id;

    @Column(name = "sucursal_id", nullable = false, updatable = false)
    private UUID sucursalId;

    @Column(name = "usuario_id", nullable = false, updatable = false)
    private UUID usuarioId;

    @Column(name = "nombre_completo", nullable = false, length = 201)
    private String nombreCompleto;

    @Column(nullable = false, length = 150)
    private String correo;

    @Enumerated(EnumType.STRING)
    @Column(name = "rol_codigo", nullable = false, length = 30, updatable = false)
    private RolPersonal rol;

    @Column(nullable = false)
    private boolean activo;

    @Column(name = "fecha_asignacion", nullable = false, updatable = false)
    private LocalDate fechaAsignacion;

    @Column(name = "fecha_desasignacion")
    private LocalDate fechaDesasignacion;

    @Version
    private Integer version;

    public static PersonalSucursalEntidad asignar(UUID id, UUID sucursalId, UUID usuarioId, String nombreCompleto,
                                                  String correo, RolPersonal rol, LocalDate fechaAsignacion) {
        PersonalSucursalEntidad personal = new PersonalSucursalEntidad();
        personal.id = id;
        personal.sucursalId = sucursalId;
        personal.usuarioId = usuarioId;
        personal.nombreCompleto = nombreCompleto;
        personal.correo = correo;
        personal.rol = rol;
        personal.activo = true;
        personal.fechaAsignacion = fechaAsignacion;
        return personal;
    }

    /** @throws gt.cunoc.proyecto2.comun.errores.SolicitudInvalidaException si la fecha es anterior a la asignación */
    public void desactivar(LocalDate fecha) {
        if (fecha.isBefore(fechaAsignacion)) {
            throw ErrorPersonal.FECHA_DESASIGNACION_INVALIDA.excepcion(fechaAsignacion);
        }
        this.activo = false;
        this.fechaDesasignacion = fecha;
    }

    /**
     * Desactiva esta asignación y devuelve la nueva en la sucursal destino (mismo usuario y rol).
     * Hay que guardar primero esta (libera el índice único de usuario activo) y luego la nueva.
     */
    public PersonalSucursalEntidad trasladarA(UUID sucursalDestinoId, LocalDate fecha) {
        desactivar(fecha);
        return asignar(UUID.randomUUID(), sucursalDestinoId, usuarioId, nombreCompleto, correo, rol, fecha);
    }

    public boolean esAdminSucursal() {
        return rol == RolPersonal.ADMIN_SUCURSAL;
    }
}
