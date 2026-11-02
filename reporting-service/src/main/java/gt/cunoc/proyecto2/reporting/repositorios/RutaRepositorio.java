package gt.cunoc.proyecto2.reporting.repositorios;

import gt.cunoc.proyecto2.reporting.modelos.RutaEntidad;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a las rutas del modelo de lectura de reportes.
 *
 * <p>No necesita consultas propias: los reportes leen las rutas por su identificador o las recorren
 * completas.
 */
public interface RutaRepositorio extends JpaRepository<RutaEntidad, UUID> {
}
