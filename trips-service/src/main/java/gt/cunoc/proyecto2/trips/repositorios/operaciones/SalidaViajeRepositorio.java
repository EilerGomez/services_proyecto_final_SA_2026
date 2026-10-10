package gt.cunoc.proyecto2.trips.repositorios.operaciones;

import gt.cunoc.proyecto2.trips.modelos.operaciones.SalidaViajeEntidad;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** El id es el viajeId. */
@Repository
public interface SalidaViajeRepositorio extends JpaRepository<SalidaViajeEntidad, UUID> {
}