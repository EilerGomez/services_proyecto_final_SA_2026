package gt.cunoc.proyecto2.sales.repositorios.viajes;

import gt.cunoc.proyecto2.sales.modelos.viajes.VistaViajeEntidad;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VistaViajeRepositorio extends JpaRepository<VistaViajeEntidad, UUID> {
}