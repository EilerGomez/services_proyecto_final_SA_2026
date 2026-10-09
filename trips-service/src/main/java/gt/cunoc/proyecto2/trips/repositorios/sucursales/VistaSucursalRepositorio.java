package gt.cunoc.proyecto2.trips.repositorios.sucursales;

import gt.cunoc.proyecto2.trips.modelos.sucursales.VistaSucursalEntidad;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/** Solo consultas: la tabla la escriben los manejadores de eventos. */
@Repository
public interface VistaSucursalRepositorio extends JpaRepository<VistaSucursalEntidad, UUID> {
}