package gt.cunoc.proyecto2.reporting.repositorios;

import gt.cunoc.proyecto2.reporting.modelos.BusEntidad;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Acceso a los buses del modelo de lectura de reportes.
 *
 * <p>Permite listar los buses de una sucursal ordenados por placa, que es lo que necesitan los
 * reportes de flota.
 */
public interface BusRepositorio extends JpaRepository<BusEntidad, UUID> {

    List<BusEntidad> findBySucursalIdOrderByPlacaAsc(UUID sucursalId);
}
