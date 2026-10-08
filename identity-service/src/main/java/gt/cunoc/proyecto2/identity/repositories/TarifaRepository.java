package gt.cunoc.proyecto2.identity.repositories;

import gt.cunoc.proyecto2.identity.models.TarifaDepreciacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface TarifaRepository extends JpaRepository<TarifaDepreciacion, UUID> {

    Optional<TarifaDepreciacion> findTopByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(LocalDate fecha);

    Page<TarifaDepreciacion> findAllByOrderByVigenteDesdeDesc(Pageable paginado);

}
