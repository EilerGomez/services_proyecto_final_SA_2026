package gt.cunoc.proyecto2.sales.repositorios.alquileres;

import gt.cunoc.proyecto2.sales.modelos.alquileres.OrdenAlquilerEntidad;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface OrdenAlquilerRepositorio extends JpaRepository<OrdenAlquilerEntidad, UUID>,
        JpaSpecificationExecutor<OrdenAlquilerEntidad> {

    /** Historial por NIT: igual que en las ventas, sin distinguir mayúsculas. */
    List<OrdenAlquilerEntidad> findByClienteNitIgnoreCaseOrderByFechaCreacionDesc(String nit);

    List<OrdenAlquilerEntidad> findByClienteUsuarioIdOrderByFechaCreacionDesc(UUID clienteUsuarioId);

    boolean existsByAlquilerId(UUID alquilerId);
}
