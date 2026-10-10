package gt.cunoc.proyecto2.sales.repositorios.asientos;

import gt.cunoc.proyecto2.sales.modelos.asientos.AsientoViajeEntidad;
import gt.cunoc.proyecto2.sales.modelos.asientos.AsientoViajeId;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AsientoViajeRepositorio extends JpaRepository<AsientoViajeEntidad, AsientoViajeId> {

    List<AsientoViajeEntidad> findByViajeIdOrderByNumeroAsientoAsc(UUID viajeId);

    @Modifying
    @Query(value = """
            UPDATE asientos_viaje
               SET estado = 'VENDIDO', venta_id = :ventaId, reservado_hasta = NULL, version = version + 1
             WHERE viaje_id = :viajeId
               AND numero_asiento IN (:asientos)
               AND (estado = 'DISPONIBLE' OR (estado = 'RESERVADO' AND reservado_hasta < now()))""",
            nativeQuery = true)
    int vender(@Param("viajeId") UUID viajeId, @Param("asientos") Collection<Integer> asientos,
               @Param("ventaId") UUID ventaId);

    @Modifying
    @Query(value = """
            UPDATE asientos_viaje
               SET estado = 'RESERVADO', venta_id = :ventaId, reservado_hasta = :hasta, version = version + 1
             WHERE viaje_id = :viajeId
               AND numero_asiento IN (:asientos)
               AND (estado = 'DISPONIBLE' OR (estado = 'RESERVADO' AND reservado_hasta < now()))""",
            nativeQuery = true)
    int reservar(@Param("viajeId") UUID viajeId, @Param("asientos") Collection<Integer> asientos,
                 @Param("ventaId") UUID ventaId, @Param("hasta") OffsetDateTime hasta);

    @Modifying
    @Query(value = """
            UPDATE asientos_viaje
               SET estado = 'DISPONIBLE', venta_id = NULL, reservado_hasta = NULL, version = version + 1
             WHERE venta_id = :ventaId""",
            nativeQuery = true)
    int liberarDeVenta(@Param("ventaId") UUID ventaId);

    @Modifying
    @Query(value = """
            UPDATE asientos_viaje
               SET estado = 'DISPONIBLE', venta_id = NULL, reservado_hasta = NULL, version = version + 1
             WHERE estado = 'RESERVADO' AND reservado_hasta < now()""",
            nativeQuery = true)
    int liberarReservasVencidas();
}