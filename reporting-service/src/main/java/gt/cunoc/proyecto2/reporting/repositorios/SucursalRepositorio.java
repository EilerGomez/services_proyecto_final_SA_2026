package gt.cunoc.proyecto2.reporting.repositorios;

import gt.cunoc.proyecto2.reporting.modelos.SucursalEntidad;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a las sucursales del modelo de lectura de reportes.
 *
 * <p>Además de validar que una sucursal exista, permite listarlas por nombre para el mapa de rutas y
 * para completar los reportes globales.
 */
public interface SucursalRepositorio extends JpaRepository<SucursalEntidad, UUID> {

    List<SucursalEntidad> findAllByOrderByNombreAsc();
}
