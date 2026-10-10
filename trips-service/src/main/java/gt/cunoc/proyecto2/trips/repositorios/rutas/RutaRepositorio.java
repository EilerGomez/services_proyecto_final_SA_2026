package gt.cunoc.proyecto2.trips.repositorios.rutas;

import gt.cunoc.proyecto2.trips.modelos.rutas.RutaEntidad;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RutaRepositorio extends JpaRepository<RutaEntidad, UUID> {

    @Query("""
            SELECT r FROM Ruta r
            WHERE r.sucursalOrigenId = COALESCE(:sucursalOrigenId, r.sucursalOrigenId)
              AND (:activa IS NULL OR r.activa = :activa)""")
    List<RutaEntidad> buscar(@Param("sucursalOrigenId") UUID sucursalOrigenId, @Param("activa") Boolean activa);

    boolean existsBySucursalOrigenIdAndSucursalDestinoId(UUID sucursalOrigenId, UUID sucursalDestinoId);
}