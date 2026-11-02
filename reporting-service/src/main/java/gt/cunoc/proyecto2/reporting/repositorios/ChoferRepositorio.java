package gt.cunoc.proyecto2.reporting.repositorios;

import gt.cunoc.proyecto2.reporting.modelos.ChoferEntidad;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a los choferes del modelo de lectura de reportes.
 *
 * <p>Permite listar los choferes de una sucursal ordenados por nombre, que es lo que necesitan los
 * reportes de personal.
 */
public interface ChoferRepositorio extends JpaRepository<ChoferEntidad, UUID> {

    List<ChoferEntidad> findBySucursalIdOrderByNombreCompletoAsc(UUID sucursalId);
}
