package gt.cunoc.proyecto2.reporting.repositorios;

import gt.cunoc.proyecto2.reporting.modelos.SucursalEntidad;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SucursalRepositorio extends JpaRepository<SucursalEntidad, UUID> {

    List<SucursalEntidad> findAllByOrderByNombreAsc();
}
