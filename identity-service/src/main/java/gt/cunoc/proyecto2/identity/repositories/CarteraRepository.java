package gt.cunoc.proyecto2.identity.repositories;

import gt.cunoc.proyecto2.identity.models.Cartera;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface CarteraRepository extends JpaRepository<Cartera, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Cartera> findWithLockByUsuarioId(UUID usuarioId);

    Optional<Cartera> findByUsuarioId(UUID usuarioId);

}
