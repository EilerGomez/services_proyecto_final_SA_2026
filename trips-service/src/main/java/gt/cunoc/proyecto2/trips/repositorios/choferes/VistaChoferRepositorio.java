package gt.cunoc.proyecto2.trips.repositorios.choferes;

import gt.cunoc.proyecto2.trips.modelos.choferes.VistaChoferEntidad;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VistaChoferRepositorio extends JpaRepository<VistaChoferEntidad, UUID> {
}