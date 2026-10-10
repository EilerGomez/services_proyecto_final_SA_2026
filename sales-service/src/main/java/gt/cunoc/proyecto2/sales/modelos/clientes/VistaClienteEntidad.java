package gt.cunoc.proyecto2.sales.modelos.clientes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

@Entity(name = "VistaCliente")
@Table(name = "vista_clientes")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class VistaClienteEntidad {

    @Id
    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(nullable = false, length = 20)
    private String nit;

    @Column(nullable = false, length = 201)
    private String nombre;

    @Column(nullable = false, length = 150)
    private String correo;
}