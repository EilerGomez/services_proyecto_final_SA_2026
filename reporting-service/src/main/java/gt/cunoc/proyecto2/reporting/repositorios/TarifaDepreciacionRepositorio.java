package gt.cunoc.proyecto2.reporting.repositorios;

import gt.cunoc.proyecto2.reporting.modelos.TarifaDepreciacionEntidad;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a las tarifas de depreciación del modelo de lectura.
 *
 * <p>Permite consultar la tarifa exacta de una fecha o la última tarifa vigente hasta una fecha
 * dada.
 */
public interface TarifaDepreciacionRepositorio extends JpaRepository<TarifaDepreciacionEntidad, UUID> {

    Optional<TarifaDepreciacionEntidad> findByVigenteDesde(LocalDate vigenteDesde);

    Optional<TarifaDepreciacionEntidad> findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(
            LocalDate fecha);
}
