package gt.cunoc.proyecto2.maintenance.repositorios;

import gt.cunoc.proyecto2.maintenance.modelos.CategoriaEntidad;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepositorio extends JpaRepository<CategoriaEntidad, UUID> {

    boolean existsByNombreIgnoreCase(String nombre);

    List<CategoriaEntidad> findAllByOrderByNombreAsc();
}
