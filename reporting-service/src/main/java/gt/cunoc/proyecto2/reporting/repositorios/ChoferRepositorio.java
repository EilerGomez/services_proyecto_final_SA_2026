package gt.cunoc.proyecto2.reporting.repositorios;

import gt.cunoc.proyecto2.reporting.modelos.ChoferEntidad;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChoferRepositorio extends JpaRepository<ChoferEntidad, UUID> {

    List<ChoferEntidad> findBySucursalIdOrderByNombreCompletoAsc(UUID sucursalId);
}
