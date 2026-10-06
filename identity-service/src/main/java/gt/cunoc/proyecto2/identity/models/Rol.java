package gt.cunoc.proyecto2.identity.models;


import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;


/**
 * Catálogo de roles (precargados en la V1).
 */
@Getter
@Setter
@Entity
@Table(name = "roles")
public class Rol {

    @Id
    @Column(name = "codigo", length = 30)
    private String codigo;

    @Column(name = "nombre", length = 60, nullable = false)
    private String nombre;

    @Column(name = "descripcion", length = 255)
    private String descripcion;
}
