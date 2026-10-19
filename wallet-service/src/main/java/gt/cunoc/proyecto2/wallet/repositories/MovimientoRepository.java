package gt.cunoc.proyecto2.wallet.repositories;

import gt.cunoc.proyecto2.wallet.models.MovimientoCartera;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface MovimientoRepository extends JpaRepository<MovimientoCartera, UUID> {

    Optional<MovimientoCartera> findByTipoAndReferenciaId(String tipo, UUID referenciaId);

    Page<MovimientoCartera> findByCarteraIdAndFechaMovimientoBetweenOrderByFechaMovimientoDesc(
            UUID carteraId, LocalDate desde, LocalDate hasta, Pageable paginado);

    Page<MovimientoCartera> findByCarteraIdAndTipoAndFechaMovimientoBetweenOrderByFechaMovimientoDesc(
            UUID carteraId, String tipo, LocalDate desde, LocalDate hasta, Pageable paginado);

    /** Suma de los movimientos de un tipo en una cartera (0 si no hay). */
    @Query("""
            SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoCartera m
            WHERE m.carteraId = :carteraId AND m.tipo = :tipo""")
    BigDecimal sumarPorTipo(@Param("carteraId") UUID carteraId, @Param("tipo") String tipo);
}
