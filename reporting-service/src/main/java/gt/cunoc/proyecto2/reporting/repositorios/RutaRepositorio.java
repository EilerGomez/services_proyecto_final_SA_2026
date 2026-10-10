package gt.cunoc.proyecto2.reporting.repositorios;

import gt.cunoc.proyecto2.reporting.modelos.RutaEntidad;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RutaRepositorio extends JpaRepository<RutaEntidad, UUID> {
}
