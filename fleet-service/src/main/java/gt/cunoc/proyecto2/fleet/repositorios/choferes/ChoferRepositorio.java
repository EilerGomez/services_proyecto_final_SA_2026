package gt.cunoc.proyecto2.fleet.repositorios.choferes;

import gt.cunoc.proyecto2.fleet.modelos.choferes.ChoferEntidad;
import gt.cunoc.proyecto2.fleet.modelos.choferes.EstadoChofer;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ChoferRepositorio extends JpaRepository<ChoferEntidad, UUID> {

    @Query("""
            SELECT c FROM Chofer c
            WHERE (:sucursalId IS NULL OR c.sucursalId = :sucursalId)
              AND (:estado IS NULL OR c.estado = :estado)
            ORDER BY c.nombreCompleto ASC""")
    List<ChoferEntidad> buscar(@Param("sucursalId") UUID sucursalId, @Param("estado") EstadoChofer estado);

    boolean existsByNumeroLicencia(String numeroLicencia);

    boolean existsByNumeroLicenciaAndIdNot(String numeroLicencia, UUID id);
}