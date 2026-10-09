package gt.cunoc.proyecto2.trips.repositorios.alquileres;

import gt.cunoc.proyecto2.trips.modelos.alquileres.AlquilerPrivadoEntidad;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface AlquilerPrivadoRepositorio extends JpaRepository<AlquilerPrivadoEntidad, UUID>,
        JpaSpecificationExecutor<AlquilerPrivadoEntidad> {

    /** T18: los del cliente, de la salida más lejana a la más antigua. */
    List<AlquilerPrivadoEntidad> findByClienteUsuarioIdOrderByFechaSalidaDesc(UUID clienteUsuarioId);
}