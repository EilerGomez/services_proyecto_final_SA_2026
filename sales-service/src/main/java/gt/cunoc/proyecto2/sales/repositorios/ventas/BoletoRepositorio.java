package gt.cunoc.proyecto2.sales.repositorios.ventas;

import gt.cunoc.proyecto2.sales.modelos.ventas.BoletoEntidad;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BoletoRepositorio extends JpaRepository<BoletoEntidad, UUID> {

    List<BoletoEntidad> findByVentaIdOrderByNumeroAsientoAsc(UUID ventaId);

    List<BoletoEntidad> findByVentaIdIn(Collection<UUID> ventaIds);
}