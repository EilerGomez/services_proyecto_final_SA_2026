package gt.cunoc.proyecto2.identity.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

/**
 * Perfil público del usuario (datos personales).
 */
@Getter
@Setter
@Entity
@Table(name = "perfiles")
public class Perfil {

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @MapsId
    @OneToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;

    @Column(name = "nombres", length = 100, nullable = false)
    private String nombres;

    @Column(name = "apellidos", length = 100, nullable = false)
    private String apellidos;

    @Column(name = "nit", length = 20, unique = true)
    private String nit;

    @Column(name = "dpi", length = 13, unique = true)
    private String dpi;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "direccion", length = 255)
    private String direccion;

    @Column(name = "foto_url", length = 500)
    private String fotoUrl;
}
