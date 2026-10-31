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

    /**
     * T14. ¿El bus ya viene EN_RUTA en otro viaje? Un bus no puede arrancar dos viajes a la vez, y
     * es lo que pasa con una vuelta cuya ida todavía no ha llegado.
     */
    boolean existsByBusIdAndEstadoAndIdNot(UUID busId, EstadoViaje estado, UUID viajeId);

    /** T14. Lo mismo para el piloto: no puede ir manejando dos viajes al mismo tiempo. */
    boolean existsByChoferIdAndEstadoAndIdNot(UUID choferId, EstadoViaje estado, UUID viajeId);

    /**
     * T14. ¿El piloto o el bus tienen otro viaje programado que sale antes que este? Los viajes se
     * operan en orden: primero sale el más próximo. Si quedó uno viejo sin registrar, se registra o
     * se elimina antes de arrancar el siguiente.
     */
    @Query("""
            SELECT CASE WHEN COUNT(v) > 0 THEN true ELSE false END FROM Viaje v
            WHERE v.id <> :viajeId
              AND v.estado = :estado
              AND (v.choferId = :choferId OR v.busId = :busId)
              AND v.fechaSalidaProgramada < :salida""")
    boolean existeOtroAntesDeEste(@Param("choferId") UUID choferId, @Param("busId") UUID busId,
                                  @Param("viajeId") UUID viajeId, @Param("estado") EstadoViaje estado,
                                  @Param("salida") OffsetDateTime salida);

    List<ViajeEntidad> findByAlquilerIdIn(Collection<UUID> alquilerIds);

    boolean existsByRutaId(UUID rutaId);
}