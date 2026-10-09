package gt.cunoc.proyecto2.trips.repositorios.viajes;

import gt.cunoc.proyecto2.trips.modelos.viajes.EstadoViaje;
import gt.cunoc.proyecto2.trips.modelos.viajes.ViajeEntidad;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/** Las búsquedas con filtros opcionales (T8, T13) usan EspecificacionesViaje. */
@Repository
public interface ViajeRepositorio extends JpaRepository<ViajeEntidad, UUID>, JpaSpecificationExecutor<ViajeEntidad> {

    @Query("""
            SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM Viaje v
            WHERE v.busId = :busId
              AND v.id <> :viajeId
              AND v.estado IN :estados
              AND v.fechaSalidaProgramada < :llegada
              AND v.fechaLlegadaEstimada > :salida""")
    boolean existeChoqueDeBus(@Param("busId") UUID busId, @Param("viajeId") UUID viajeId,
                              @Param("estados") Collection<EstadoViaje> estados,
                              @Param("salida") OffsetDateTime salida, @Param("llegada") OffsetDateTime llegada);

    @Query("""
            SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM Viaje v
            WHERE v.choferId = :choferId
              AND v.id <> :viajeId
              AND v.estado IN :estados
              AND v.fechaSalidaProgramada < :llegada
              AND v.fechaLlegadaEstimada > :salida""")
    boolean existeChoqueDeChofer(@Param("choferId") UUID choferId, @Param("viajeId") UUID viajeId,
                                 @Param("estados") Collection<EstadoViaje> estados,
                                 @Param("salida") OffsetDateTime salida, @Param("llegada") OffsetDateTime llegada);

    List<ViajeEntidad> findByBusIdAndEstadoInOrderByFechaSalidaProgramadaAsc(UUID busId,
                                                                           Collection<EstadoViaje> estados);

    List<ViajeEntidad> findByChoferIdAndEstadoInOrderByFechaSalidaProgramadaAsc(UUID choferId,
                                                                              Collection<EstadoViaje> estados);

    List<ViajeEntidad> findByAlquilerIdIn(Collection<UUID> alquilerIds);

    boolean existsByRutaId(UUID rutaId);
}