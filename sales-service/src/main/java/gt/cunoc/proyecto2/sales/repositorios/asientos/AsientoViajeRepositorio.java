package gt.cunoc.proyecto2.sales.repositorios.asientos;

import gt.cunoc.proyecto2.sales.modelos.asientos.AsientoViajeEntidad;
import gt.cunoc.proyecto2.sales.modelos.asientos.AsientoViajeId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AsientoViajeRepositorio extends JpaRepository<AsientoViajeEntidad, AsientoViajeId> {

    List<AsientoViajeEntidad> findByViajeIdOrderByNumeroAsientoAsc(UUID viajeId);
}