package gt.cunoc.proyecto2.identity.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Cuenta de usuario (credenciales y estado).
 *
 * <p>Los usuarios no se eliminan, solo se desactivan.
 */
@Getter
@Setter
@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private UUID id;

    @Column(name = "correo", length = 150, nullable = false, unique = true)
    private String correo;

    /**
     * Hash BCrypt. Null en cuentas OAuth (GOOGLE), que no tienen contraseña.
     * iniciarSesion y cambiarContrasena deben rechazar cuentas sin clave.
     */
    @Column(name = "contrasena_hash", length = 255)
    private String claveCifrada;

    /** Origen de la cuenta: LOCAL (registro clásico) o GOOGLE (OAuth2). */
    @Column(name = "proveedor", length = 20, nullable = false)
    private String proveedor = "LOCAL";

    /** Identificador externo (sub de Google). Null en cuentas LOCAL. */
    @Column(name = "proveedor_id", length = 255)
    private String proveedorId;

    @Column(name = "estado", length = 15, nullable = false)
    private String estado = "ACTIVO";

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private Instant bloqueadoHasta;

    @Column(name = "chofer_id")
    private UUID choferId;

    @Column(name = "sucursal_id")
    private UUID sucursalId;

    @Column(name = "doble_factor_activo", nullable = false)
    private boolean dobleFactorActivo = false;

    @Column(name = "ultimo_acceso")
    private Instant ultimoAcceso;

    /** Los JWT emitidos antes de este momento ya no valen (sesiones revocadas). Null = nunca se revocó. */
    @Column(name = "tokens_validos_desde")
    private Instant tokensValidosDesde;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "usuario_roles",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_codigo"))
    private Set<Rol> roles = new HashSet<>();

    /**
     * Revoca de inmediato todos los JWT que ya se emitieron: desde el siguiente segundo completo
     * (el iat del token va en segundos), así ningún token emitido antes de este instante queda válido.
     * Devuelve la nueva fecha.
     */
    public Instant revocarTokens(Instant ahora) {
        Instant segundo = ahora.truncatedTo(ChronoUnit.SECONDS);
        tokensValidosDesde = segundo.equals(ahora) ? segundo : segundo.plusSeconds(1);
        return tokensValidosDesde;
    }
}
