package gt.cunoc.proyecto2.trips.repositorios.tarifas;

import gt.cunoc.proyecto2.trips.modelos.tarifas.VistaTarifaDepreciacionEntidad;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VistaTarifaDepreciacionRepositorio extends JpaRepository<VistaTarifaDepreciacionEntidad, UUID> {

    Optional<VistaTarifaDepreciacionEntidad> findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(
            LocalDate fecha);
}