package gt.cunoc.proyecto2.sales.repositorios.ventas;

import gt.cunoc.proyecto2.sales.modelos.ventas.VentaEntidad;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface VentaRepositorio extends JpaRepository<VentaEntidad, UUID>, JpaSpecificationExecutor<VentaEntidad> {
}