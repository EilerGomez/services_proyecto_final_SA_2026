package gt.cunoc.proyecto2.fleet.repositorios.bitacora;

import gt.cunoc.proyecto2.fleet.modelos.bitacora.BitacoraKilometrajeEntidad;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface BitacoraKilometrajeRepositorio extends JpaRepository<BitacoraKilometrajeEntidad, UUID> {

    List<BitacoraKilometrajeEntidad> findTop5ByBusIdOrderByFechaLlegadaDesc(UUID busId);

    @Query("""
            SELECT b FROM BitacoraKilometraje b
            WHERE b.busId = :busId
              AND b.fechaLlegada >= :desde
              AND b.fechaLlegada < :hasta
            ORDER BY b.fechaLlegada DESC""")
    List<BitacoraKilometrajeEntidad> buscarPorRango(@Param("busId") UUID busId,
                                                    @Param("desde") OffsetDateTime desde,
                                                    @Param("hasta") OffsetDateTime hasta);

    boolean existsByViajeId(UUID viajeId);
}