package gt.cunoc.proyecto2.maintenance.repositorios;

import gt.cunoc.proyecto2.maintenance.modelos.VistaBusEntidad;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VistaBusRepositorio extends JpaRepository<VistaBusEntidad, UUID> {

    List<VistaBusEntidad> findBySucursalIdOrderByPlacaAsc(UUID sucursalId);
}
