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

    /** Del más reciente al más antiguo: por fecha del movimiento y, en el mismo día, por la hora en que se registró. */
    @Query(value = """
            SELECT m FROM MovimientoCartera m
            WHERE m.carteraId = :carteraId AND m.fechaMovimiento BETWEEN :desde AND :hasta
            ORDER BY m.fechaMovimiento DESC, m.fechaCreacion DESC, m.id""",
            countQuery = """
            SELECT COUNT(m) FROM MovimientoCartera m
            WHERE m.carteraId = :carteraId AND m.fechaMovimiento BETWEEN :desde AND :hasta""")
    Page<MovimientoCartera> findByCarteraIdAndFechaMovimientoBetweenOrderByFechaMovimientoDesc(
            @Param("carteraId") UUID carteraId, @Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta,
            Pageable paginado);

    /** Igual, de un solo tipo (billetera del sistema: COBRO o DEVOLUCION). */
    @Query(value = """
            SELECT m FROM MovimientoCartera m
            WHERE m.carteraId = :carteraId AND m.tipo = :tipo AND m.fechaMovimiento BETWEEN :desde AND :hasta
            ORDER BY m.fechaMovimiento DESC, m.fechaCreacion DESC, m.id""",
            countQuery = """
            SELECT COUNT(m) FROM MovimientoCartera m
            WHERE m.carteraId = :carteraId AND m.tipo = :tipo AND m.fechaMovimiento BETWEEN :desde AND :hasta""")
    Page<MovimientoCartera> findByCarteraIdAndTipoAndFechaMovimientoBetweenOrderByFechaMovimientoDesc(
            @Param("carteraId") UUID carteraId, @Param("tipo") String tipo, @Param("desde") LocalDate desde,
            @Param("hasta") LocalDate hasta, Pageable paginado);

    /** Suma de los movimientos de un tipo en una cartera (0 si no hay). */
    @Query("""
            SELECT COALESCE(SUM(m.monto), 0) FROM MovimientoCartera m
            WHERE m.carteraId = :carteraId AND m.tipo = :tipo""")
    BigDecimal sumarPorTipo(@Param("carteraId") UUID carteraId, @Param("tipo") String tipo);
}
