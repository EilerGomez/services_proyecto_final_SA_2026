package gt.cunoc.proyecto2.sales.repositorios.clientes;

import gt.cunoc.proyecto2.sales.modelos.clientes.VistaClienteEntidad;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VistaClienteRepositorio extends JpaRepository<VistaClienteEntidad, UUID> {

    Optional<VistaClienteEntidad> findFirstByNit(String nit);
}