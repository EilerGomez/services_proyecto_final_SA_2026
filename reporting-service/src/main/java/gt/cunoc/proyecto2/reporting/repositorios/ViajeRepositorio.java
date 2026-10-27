package gt.cunoc.proyecto2.reporting.repositorios;

import gt.cunoc.proyecto2.reporting.modelos.ViajeEntidad;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ViajeRepositorio extends JpaRepository<ViajeEntidad, UUID> {

    List<ViajeEntidad> findByBusIdAndFechaSalidaBetweenOrderByFechaSalidaDesc(
            UUID busId, OffsetDateTime desde, OffsetDateTime hasta);

    /** RS4: los viajes PRIVADO de estos alquileres, para poner la placa del bus en el reporte. */
    List<ViajeEntidad> findByAlquilerIdIn(Collection<UUID> alquilerIds);

    // OJO: no usar "(:desde IS NULL OR ...)" con OffsetDateTime: PostgreSQL no infiere el tipo
    // del parámetro nulo ("could not determine data type of parameter"). Como fecha_salida es
    // NOT NULL, COALESCE es equivalente y el parámetro hereda el tipo de la columna.
    @Query("""
            SELECT v FROM ReporteViaje v
            WHERE (:sucursalId IS NULL OR v.sucursalId = :sucursalId)
              AND (:busId IS NULL OR v.busId = :busId)
              AND v.fechaSalida >= COALESCE(:desde, v.fechaSalida)
              AND v.fechaSalida <= COALESCE(:hasta, v.fechaSalida)
            ORDER BY v.fechaSalida DESC
            """)
    List<ViajeEntidad> buscar(@Param("sucursalId") UUID sucursalId,
                             @Param("busId") UUID busId,
                             @Param("desde") OffsetDateTime desde,
                             @Param("hasta") OffsetDateTime hasta);

    /** Chofer del viaje EN_RUTA del bus o, si no hay, del próximo PROGRAMADO. */
    @Query("""
            SELECT v.choferId FROM ReporteViaje v
            WHERE v.busId = :busId AND v.estado IN ('EN_RUTA', 'PROGRAMADO')
            ORDER BY CASE WHEN v.estado = 'EN_RUTA' THEN 0 ELSE 1 END, v.fechaSalida ASC
            """)
    List<UUID> choferActualDe(@Param("busId") UUID busId, Pageable primero);
}
