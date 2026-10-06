package gt.cunoc.proyecto2.identity.repositories;


import gt.cunoc.proyecto2.identity.models.TokenRefresco;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;


public interface TokenRefrescoRepository extends JpaRepository<TokenRefresco, UUID> {


    Optional<TokenRefresco> findByTokenHash(String tokenHash);

}
