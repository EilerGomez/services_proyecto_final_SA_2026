package gt.cunoc.proyecto2.identity.repositories;


import gt.cunoc.proyecto2.identity.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;


public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {


    Optional<Usuario> findByCorreo(String correo);


    Optional<Usuario> findByChoferId(UUID choferId);


    List<Usuario> findBySucursalIdAndRoles_Codigo(UUID sucursalId, String rolCodigo);


    boolean existsByCorreo(String correo);


    long countByEstadoAndRoles_Codigo(String estado, String rolCodigo);

}
