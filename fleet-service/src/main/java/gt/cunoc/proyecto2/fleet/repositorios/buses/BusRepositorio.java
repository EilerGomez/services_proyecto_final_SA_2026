package gt.cunoc.proyecto2.fleet.repositorios.buses;

import gt.cunoc.proyecto2.fleet.modelos.buses.BusEntidad;
import gt.cunoc.proyecto2.fleet.modelos.buses.EstadoBus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BusRepositorio extends JpaRepository<BusEntidad, UUID> {

    @Query("""
            SELECT b FROM Bus b
            WHERE (:sucursalId IS NULL OR b.sucursalId = :sucursalId)
              AND (:estado IS NULL OR b.estado = :estado)
            ORDER BY b.placa ASC""")
    List<BusEntidad> buscar(@Param("sucursalId") UUID sucursalId, @Param("estado") EstadoBus estado);

    boolean existsByPlaca(String placa);

    boolean existsBySucursalIdAndEstadoNot(UUID sucursalId, EstadoBus estado);
}