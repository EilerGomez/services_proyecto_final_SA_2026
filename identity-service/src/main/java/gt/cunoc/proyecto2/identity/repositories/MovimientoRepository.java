package gt.cunoc.proyecto2.identity.repositories;


import gt.cunoc.proyecto2.identity.models.MovimientoCartera;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;


public interface MovimientoRepository extends JpaRepository<MovimientoCartera, UUID> {


    Optional<MovimientoCartera> findByTipoAndReferenciaId(String tipo, UUID referenciaId);


    Page<MovimientoCartera> findByCarteraIdAndFechaMovimientoBetween(
            UUID carteraId, LocalDate desde, LocalDate hasta, Pageable paginado);

}
