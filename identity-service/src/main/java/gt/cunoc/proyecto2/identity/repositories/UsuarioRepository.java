package gt.cunoc.proyecto2.identity.repositories;

import gt.cunoc.proyecto2.identity.models.Usuario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByCorreo(String correo);

    Optional<Usuario> findByProveedorAndProveedorId(String proveedor, String proveedorId);

    Optional<Usuario> findByChoferId(UUID choferId);

    List<Usuario> findBySucursalIdAndRoles_Codigo(UUID sucursalId, String rolCodigo);

    boolean existsByCorreo(String correo);

    List<Usuario> findByEstadoOrderByCorreoAsc(String estado);

    long countByEstadoAndRoles_Codigo(String estado, String rolCodigo);

    @Query("""
            SELECT DISTINCT u FROM Usuario u LEFT JOIN Perfil p ON p.usuarioId = u.id
            WHERE (:rol IS NULL OR EXISTS (SELECT 1 FROM u.roles r WHERE r.codigo = :rol))
              AND (:estado IS NULL OR u.estado = :estado)
              AND (CAST(:q AS String) IS NULL
                OR LOWER(u.correo) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%'))
                OR LOWER(p.nombres) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%'))
                OR LOWER(p.apellidos) LIKE LOWER(CONCAT('%', CAST(:q AS String), '%')))
            """)
    Page<Usuario> buscar(String rol, String estado, String q, Pageable paginado);

}
