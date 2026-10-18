package gt.cunoc.proyecto2.reporting.repositorios;

import gt.cunoc.proyecto2.reporting.modelos.IngresoEntidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IngresoRepositorio extends JpaRepository<IngresoEntidad, UUID> {

    boolean existsByEventoOrigenId(UUID eventoOrigenId);

    List<IngresoEntidad> findByReferenciaIdOrderByFechaNegocioDesc(UUID referenciaId);

    @Query("""
            SELECT i FROM ReporteIngreso i
            WHERE (:sucursalId IS NULL OR i.sucursalId = :sucursalId)
              AND (:tipo IS NULL OR i.tipo = :tipo)
              AND i.fechaNegocio >= COALESCE(:desde, i.fechaNegocio)
              AND i.fechaNegocio <= COALESCE(:hasta, i.fechaNegocio)
            ORDER BY i.fechaNegocio DESC
            """)
    List<IngresoEntidad> buscar(@Param("sucursalId") UUID sucursalId,
                               @Param("tipo") String tipo,
                               @Param("desde") LocalDate desde,
                               @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT COALESCE(SUM(i.monto), 0) FROM ReporteIngreso i
            WHERE (:sucursalId IS NULL OR i.sucursalId = :sucursalId)
              AND i.fechaNegocio >= COALESCE(:desde, i.fechaNegocio)
              AND i.fechaNegocio <= COALESCE(:hasta, i.fechaNegocio)
            """)
    BigDecimal sumar(@Param("sucursalId") UUID sucursalId,
                     @Param("desde") LocalDate desde,
                     @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT i.sucursalId, COALESCE(SUM(i.monto), 0) FROM ReporteIngreso i
            WHERE i.fechaNegocio >= COALESCE(:desde, i.fechaNegocio)
              AND i.fechaNegocio <= COALESCE(:hasta, i.fechaNegocio)
            GROUP BY i.sucursalId
            """)
    List<Object[]> sumarPorSucursal(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    /** Fila positiva de una venta (para copiar ruta/bus/origen/destino en la cancelación). */
    @Query("""
            SELECT i FROM ReporteIngreso i
            WHERE i.referenciaId = :referenciaId AND i.monto > 0
            ORDER BY i.fechaNegocio DESC
            """)
    List<IngresoEntidad> filasPositivasDe(@Param("referenciaId") UUID referenciaId);

    /** RG4: boletos (las cancelaciones restan) e ingresos por ruta, orden descendente. */
    @Query("""
            SELECT i.rutaId, COALESCE(SUM(i.cantidadBoletos), 0), COALESCE(SUM(i.monto), 0)
            FROM ReporteIngreso i
            WHERE i.tipo = 'BOLETO' AND i.rutaId IS NOT NULL
              AND i.fechaNegocio >= COALESCE(:desde, i.fechaNegocio)
              AND i.fechaNegocio <= COALESCE(:hasta, i.fechaNegocio)
            GROUP BY i.rutaId
            ORDER BY SUM(i.cantidadBoletos) DESC
            """)
    List<Object[]> demandaPorRuta(@Param("desde") LocalDate desde,
                                 @Param("hasta") LocalDate hasta,
                                 org.springframework.data.domain.Pageable limite);
}
