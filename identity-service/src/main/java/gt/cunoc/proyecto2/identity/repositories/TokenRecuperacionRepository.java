package gt.cunoc.proyecto2.identity.repositories;


import gt.cunoc.proyecto2.identity.models.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, UUID> {


    Optional<TokenRecuperacion> findByTokenHash(String tokenHash);

}
