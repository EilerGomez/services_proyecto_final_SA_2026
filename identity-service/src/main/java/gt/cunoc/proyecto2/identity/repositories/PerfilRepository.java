package gt.cunoc.proyecto2.identity.repositories;


import gt.cunoc.proyecto2.identity.models.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface PerfilRepository extends JpaRepository<Perfil, UUID> {


    Optional<Perfil> findByNit(String nit);


    boolean existsByNit(String nit);


    boolean existsByDpi(String dpi);

}
