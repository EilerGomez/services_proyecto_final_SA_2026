package gt.cunoc.proyecto2.maintenance.repositorios;

import gt.cunoc.proyecto2.maintenance.modelos.ServicioTallerEntidad;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServicioRepositorio extends JpaRepository<ServicioTallerEntidad, UUID> {

    @Query("""
            SELECT DISTINCT s FROM ServicioTaller s LEFT JOIN s.lineas l
            WHERE (:sucursalId IS NULL OR s.sucursalId = :sucursalId)
              AND (:busId IS NULL OR s.busId = :busId)
              AND s.fechaServicio >= COALESCE(:desde, s.fechaServicio)
              AND s.fechaServicio <= COALESCE(:hasta, s.fechaServicio)
              AND (:categoriaId IS NULL OR l.categoriaId = :categoriaId)
            ORDER BY s.fechaServicio DESC, s.id
            """)
    List<ServicioTallerEntidad> buscar(@Param("sucursalId") UUID sucursalId,
                                       @Param("busId") UUID busId,
                                       @Param("desde") LocalDate desde,
                                       @Param("hasta") LocalDate hasta,
                                       @Param("categoriaId") UUID categoriaId);
}
