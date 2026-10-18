package gt.cunoc.proyecto2.wallet.repositories;

import gt.cunoc.proyecto2.wallet.models.MovimientoCartera;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface MovimientoRepository extends JpaRepository<MovimientoCartera, UUID> {

    Optional<MovimientoCartera> findByTipoAndReferenciaId(String tipo, UUID referenciaId);

    Page<MovimientoCartera> findByCarteraIdAndFechaMovimientoBetweenOrderByFechaMovimientoDesc(
            UUID carteraId, LocalDate desde, LocalDate hasta, Pageable paginado);
}
