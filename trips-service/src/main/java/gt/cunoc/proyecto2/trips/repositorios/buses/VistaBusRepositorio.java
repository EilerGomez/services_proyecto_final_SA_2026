package gt.cunoc.proyecto2.trips.repositorios.buses;

import gt.cunoc.proyecto2.trips.modelos.buses.VistaBusEntidad;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VistaBusRepositorio extends JpaRepository<VistaBusEntidad, UUID> {
}