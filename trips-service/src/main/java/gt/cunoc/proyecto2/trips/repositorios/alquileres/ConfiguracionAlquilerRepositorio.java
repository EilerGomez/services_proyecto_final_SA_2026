package gt.cunoc.proyecto2.trips.repositorios.alquileres;

import gt.cunoc.proyecto2.trips.modelos.alquileres.ConfiguracionAlquilerEntidad;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ConfiguracionAlquilerRepositorio extends JpaRepository<ConfiguracionAlquilerEntidad, UUID> {

    /** T1: historial, el más reciente primero. */
    List<ConfiguracionAlquilerEntidad> findAllByOrderByVigenteDesdeDesc();

    boolean existsByVigenteDesde(LocalDate vigenteDesde);

    /** Precio vigente en una fecha: el de vigente_desde más reciente que no pase de esa fecha. */
    Optional<ConfiguracionAlquilerEntidad> findFirstByVigenteDesdeLessThanEqualOrderByVigenteDesdeDesc(
            LocalDate fecha);
}