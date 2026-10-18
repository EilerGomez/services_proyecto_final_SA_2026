package gt.cunoc.proyecto2.reporting.repositorios;

import gt.cunoc.proyecto2.reporting.modelos.GastoEntidad;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface GastoRepositorio extends JpaRepository<GastoEntidad, UUID> {

    @Query("""
            SELECT COUNT(g) > 0 FROM ReporteGasto g
            WHERE g.eventoOrigenId = :eventoOrigenId AND g.tipo = :tipo
              AND COALESCE(g.lineaDetalleId, :vacio) = COALESCE(:lineaDetalleId, :vacio)
            """)
    boolean existe(@Param("eventoOrigenId") UUID eventoOrigenId,
                   @Param("tipo") String tipo,
                   @Param("lineaDetalleId") UUID lineaDetalleId,
                   @Param("vacio") UUID vacio);

    @Query("""
            SELECT g FROM ReporteGasto g
            WHERE (:sucursalId IS NULL OR g.sucursalId = :sucursalId)
              AND (:tipo IS NULL OR g.tipo = :tipo)
              AND (:busId IS NULL OR g.busId = :busId)
              AND g.fechaNegocio >= COALESCE(:desde, g.fechaNegocio)
              AND g.fechaNegocio <= COALESCE(:hasta, g.fechaNegocio)
            ORDER BY g.fechaNegocio DESC
            """)
    List<GastoEntidad> buscar(@Param("sucursalId") UUID sucursalId,
                             @Param("tipo") String tipo,
                             @Param("busId") UUID busId,
                             @Param("desde") LocalDate desde,
                             @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT COALESCE(SUM(g.monto), 0) FROM ReporteGasto g
            WHERE (:sucursalId IS NULL OR g.sucursalId = :sucursalId)
              AND g.fechaNegocio >= COALESCE(:desde, g.fechaNegocio)
              AND g.fechaNegocio <= COALESCE(:hasta, g.fechaNegocio)
            """)
    BigDecimal sumar(@Param("sucursalId") UUID sucursalId,
                     @Param("desde") LocalDate desde,
                     @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT g.sucursalId, COALESCE(SUM(g.monto), 0) FROM ReporteGasto g
            WHERE g.fechaNegocio >= COALESCE(:desde, g.fechaNegocio)
              AND g.fechaNegocio <= COALESCE(:hasta, g.fechaNegocio)
            GROUP BY g.sucursalId
            """)
    List<Object[]> sumarPorSucursal(@Param("desde") LocalDate desde, @Param("hasta") LocalDate hasta);

    @Query("""
            SELECT g.tipo, COUNT(g), COALESCE(SUM(g.monto), 0) FROM ReporteGasto g
            WHERE (:sucursalId IS NULL OR g.sucursalId = :sucursalId)
              AND g.fechaNegocio >= COALESCE(:desde, g.fechaNegocio)
              AND g.fechaNegocio <= COALESCE(:hasta, g.fechaNegocio)
            GROUP BY g.tipo ORDER BY g.tipo
            """)
    List<Object[]> sumarPorTipo(@Param("sucursalId") UUID sucursalId,
                               @Param("desde") LocalDate desde,
                               @Param("hasta") LocalDate hasta);

    /** RG2: suma por sucursal y tipo (COMBUSTIBLE, MANO_OBRA, REPUESTO, DEPRECIACION, SALARIO_CHOFER). */
    @Query("""
            SELECT g.sucursalId, g.tipo, COALESCE(SUM(g.monto), 0) FROM ReporteGasto g
            WHERE (:sucursalId IS NULL OR g.sucursalId = :sucursalId)
              AND g.fechaNegocio >= COALESCE(:desde, g.fechaNegocio)
              AND g.fechaNegocio <= COALESCE(:hasta, g.fechaNegocio)
            GROUP BY g.sucursalId, g.tipo
            """)
    List<Object[]> sumarPorSucursalYTipo(@Param("sucursalId") UUID sucursalId,
                                        @Param("desde") LocalDate desde,
                                        @Param("hasta") LocalDate hasta);

    /** RG5: detalle de cada gasto de taller (bus, fecha, descripción y monto) para agrupar por categoría. */
    @Query("""
            SELECT g.categoriaId, g.categoriaNombre, g.busId, g.fechaNegocio, g.descripcion, g.monto
            FROM ReporteGasto g
            WHERE g.tipo IN ('MANO_OBRA', 'REPUESTO')
              AND (:sucursalId IS NULL OR g.sucursalId = :sucursalId)
              AND (:busId IS NULL OR g.busId = :busId)
              AND g.fechaNegocio >= COALESCE(:desde, g.fechaNegocio)
              AND g.fechaNegocio <= COALESCE(:hasta, g.fechaNegocio)
            ORDER BY g.categoriaNombre, g.fechaNegocio
            """)
    List<Object[]> tallerDetalleCategoria(@Param("sucursalId") UUID sucursalId,
                                          @Param("busId") UUID busId,
                                          @Param("desde") LocalDate desde,
                                          @Param("hasta") LocalDate hasta);

    /**
     * RS6: detalle de cada gasto de taller del bus (categoría, fecha, descripción, tipo y monto),
     * con filtro opcional por categoría. Sin agrupar: se ven las reparaciones repetidas del bus.
     */
    @Query("""
            SELECT g.busId, g.categoriaId, g.categoriaNombre, g.fechaNegocio, g.descripcion, g.tipo, g.monto
            FROM ReporteGasto g
            WHERE g.tipo IN ('MANO_OBRA', 'REPUESTO')
              AND g.sucursalId = :sucursalId
              AND (:busId IS NULL OR g.busId = :busId)
              AND (:categoriaId IS NULL OR g.categoriaId = :categoriaId)
              AND g.fechaNegocio >= COALESCE(:desde, g.fechaNegocio)
              AND g.fechaNegocio <= COALESCE(:hasta, g.fechaNegocio)
            ORDER BY g.busId, g.categoriaNombre, g.fechaNegocio
            """)
    List<Object[]> tallerDetalleBus(@Param("sucursalId") UUID sucursalId,
                                    @Param("busId") UUID busId,
                                    @Param("categoriaId") UUID categoriaId,
                                    @Param("desde") LocalDate desde,
                                    @Param("hasta") LocalDate hasta);

    List<GastoEntidad> findByCategoriaIdOrderByFechaNegocioDesc(UUID categoriaId);
}
